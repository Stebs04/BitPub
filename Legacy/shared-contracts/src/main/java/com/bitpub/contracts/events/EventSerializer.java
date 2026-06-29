package com.bitpub.contracts.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class EventSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // For schema evolution

    public static String serialize(BaseSensorEvent event) throws JsonProcessingException {
        return MAPPER.writeValueAsString(event);
    }

    public static BaseSensorEvent deserialize(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, BaseSensorEvent.class);
    }

    public static <T extends BaseSensorEvent> T deserialize(String json, Class<T> clazz) throws JsonProcessingException {
        return MAPPER.readValue(json, clazz);
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}
