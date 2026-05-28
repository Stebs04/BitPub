package com.bitpub.common.security.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        com.bitpub.common.exception.ApiError apiError = com.bitpub.common.exception.ApiError.builder()
                .status(HttpServletResponse.SC_FORBIDDEN)
                .title(com.bitpub.common.exception.ErrorCode.FORBIDDEN.getDefaultMessage())
                .code(com.bitpub.common.exception.ErrorCode.FORBIDDEN.getCode())
                .message(accessDeniedException.getMessage())
                .path(request.getServletPath())
                .traceId(org.slf4j.MDC.get(com.bitpub.common.exception.TraceIdFilter.TRACE_ID_MDC_KEY))
                .build();

        mapper.writeValue(response.getOutputStream(), apiError);
    }
}
