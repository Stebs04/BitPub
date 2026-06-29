package com.bitpub.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Lightweight Dependency Injection Container for JavaFX.
 * Manages singletons (Services) and transient instances (ViewModels).
 */
public class DIContainer {
    private static final DIContainer instance = new DIContainer();
    
    private final Map<Class<?>, Object> singletons = new HashMap<>();
    private final Map<Class<?>, Supplier<?>> factories = new HashMap<>();

    private DIContainer() {}

    public static DIContainer getInstance() {
        return instance;
    }

    /**
     * Registers a singleton instance.
     */
    public <T> void registerSingleton(Class<T> type, T instance) {
        singletons.put(type, instance);
    }

    /**
     * Registers a factory for transient instances (e.g., ViewModels or Controllers).
     */
    public <T> void registerFactory(Class<T> type, Supplier<T> factory) {
        factories.put(type, factory);
    }

    /**
     * Resolves an instance of the requested type.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) {
        if (singletons.containsKey(type)) {
            return (T) singletons.get(type);
        }
        if (factories.containsKey(type)) {
            return (T) factories.get(type).get();
        }
        
        // Fallback for simple instantiation if it has a no-arg constructor
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not resolve dependency for: " + type.getName(), e);
        }
    }
}
