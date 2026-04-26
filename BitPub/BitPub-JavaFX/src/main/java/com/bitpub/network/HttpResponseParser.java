package com.bitpub.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

/**
 * Static utility class extending pure functions to parse HTTP responses.
 * Extracted parsing logic here ensures operations execute solely on worker threads
 * and cleanly throw custom exceptions on HTTP errors or bad formatting.
 *
 * @author Stefano Bellan 20054330
 * @version 1.0
 */
public final class HttpResponseParser {

    /** Global Gson instance used for deserialization; thread-safe for reading/parsing. */
    private static final Gson GSON = new Gson();

    private HttpResponseParser() {
        // Prevent instantiation of utility class
    }

    /**
     * Validates the HTTP response for error status codes (4xx, 5xx) before parsing.
     *
     * @param response the HTTP response to check
     * @throws HttpParsingException if the status code indicates failure
     */
    private static void requireSuccess(HttpResponse<?> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 400) {
            throw new HttpParsingException("HTTP Error: Received status code " + response.statusCode());
        }
    }

    /**
     * Parses a JSON string response body into a specific Java class instance.
     *
     * @param <T>      the type to return
     * @param response the raw HTTP response as a String
     * @param type     the target class for the JSON deserialization
     * @return the instantiated Java object
     * @throws HttpParsingException if the payload is malformed or the JSON throws an exception
     */
    public static <T> T parseJson(HttpResponse<String> response, Class<T> type) {
        requireSuccess(response);
        try {
            return GSON.fromJson(response.body(), type);
        } catch (JsonSyntaxException e) {
            throw new HttpParsingException("Failed to decode JSON response into " + type.getSimpleName(), e);
        }
    }

    /**
     * Parses a JSON string response body representing an array into a Java List.
     *
     * @param <T>      the component type of the array
     * @param response the raw HTTP response containing a JSON array
     * @param arrayType the target array class to deserialize to (e.g. MyObject[].class)
     * @return a List wrapping the parsed elements
     * @throws HttpParsingException if the array payload is malformed
     */
    public static <T> List<T> parseJsonList(HttpResponse<String> response, Class<T[]> arrayType) {
        requireSuccess(response);
        try {
            T[] array = GSON.fromJson(response.body(), arrayType);
            // Array transformation occurs on the worker thread protecting the UI
            return Arrays.asList(array);
        } catch (JsonSyntaxException | NullPointerException e) {
            throw new HttpParsingException("Failed to decode JSON array", e);
        }
    }

    /**
     * Returns the raw text response, just ensuring the HTTP call was successful.
     *
     * @param response the HTTP response
     * @return the raw string body of the response
     * @throws HttpParsingException if the status code indicates failure
     */
    public static String parseText(HttpResponse<String> response) {
        requireSuccess(response);
        return response.body();
    }
}
