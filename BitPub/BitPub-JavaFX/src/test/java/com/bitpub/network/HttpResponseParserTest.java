package com.bitpub.network;

import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HttpResponseParser.
 * Validates JSON parsing and error handling for response interpretation.
 *
 * @author Stefano Bellan 20054330
 */
public class HttpResponseParserTest {

    // Simple Dummy Class for test mapping
    public static class DummyData {
        public int id;
        public String name;
    }

    private HttpResponse<String> createMockResponse(int statusCode, String body) {
        return new HttpResponse<>() {
            @Override public int statusCode() { return statusCode; }
            @Override public HttpRequest request() { return null; }
            @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
            @Override public HttpHeaders headers() { return HttpHeaders.of(Collections.emptyMap(), (k, v) -> true); }
            @Override public String body() { return body; }
            @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
            @Override public URI uri() { return null; }
            @Override public HttpClient.Version version() { return null; }
        };
    }

    @Test
    void testParseJsonSuccess() {
        HttpResponse<String> response = createMockResponse(200, "{\"id\":1, \"name\":\"Test\"}");
        DummyData data = HttpResponseParser.parseJson(response, DummyData.class);

        assertNotNull(data);
        assertEquals(1, data.id);
        assertEquals("Test", data.name);
    }

    @Test
    void testParseJsonListSuccess() {
        HttpResponse<String> response = createMockResponse(200, "[{\"id\":1, \"name\":\"A\"}, {\"id\":2, \"name\":\"B\"}]");
        List<DummyData> list = HttpResponseParser.parseJsonList(response, DummyData[].class);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).id);
        assertEquals("A", list.get(0).name);
    }

    @Test
    void testParseJsonHttpErrorThrowsException() {
        HttpResponse<String> response = createMockResponse(404, "Not Found");
        
        HttpParsingException ex = assertThrows(HttpParsingException.class, () -> {
            HttpResponseParser.parseJson(response, DummyData.class);
        });
        
        assertTrue(ex.getMessage().contains("HTTP Error"));
    }

    @Test
    void testParseJsonMalformedException() {
        HttpResponse<String> response = createMockResponse(200, "{\"id\":1, name:\"Test\""); // Invalid JSON (unterminated object)
        
        HttpParsingException ex = assertThrows(HttpParsingException.class, () -> {
            HttpResponseParser.parseJson(response, DummyData.class);
        });
        
        assertTrue(ex.getMessage().contains("Failed to decode JSON response") || ex.getCause() instanceof Exception);
    }

    @Test
    void testParseTextSuccess() {
        HttpResponse<String> response = createMockResponse(200, "Plain text result");
        String result = HttpResponseParser.parseText(response);
        
        assertEquals("Plain text result", result);
    }
}
