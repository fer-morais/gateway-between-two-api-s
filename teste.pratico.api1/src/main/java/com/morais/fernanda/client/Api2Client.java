package com.morais.fernanda.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morais.fernanda.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class Api2Client {

    private static final Logger log = LoggerFactory.getLogger(Api2Client.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public Api2Client(RestTemplate restTemplate,
                      ObjectMapper objectMapper,
                      @Value("${api2.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    public ResponseEntity<String> forward(
            HttpMethod method,
            String pathWithQuery,
            String bearerToken,
            Object body,
            HttpServletRequest inboundRequest
    ) {
        String url = baseUrl + pathWithQuery;

        String requestId = inboundRequest != null ? inboundRequest.getHeader("X-Request-Id") : null;
        if (requestId == null || requestId.isBlank()) requestId = UUID.randomUUID().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-Request-Id", requestId);

        if (body != null) headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null && !bearerToken.isBlank()) headers.set(HttpHeaders.AUTHORIZATION, bearerToken);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);

            log.info("API2 {} {} -> {} (x-request-id={})",
                    method, pathWithQuery, response.getStatusCode().value(), requestId);

            HttpHeaders out = new HttpHeaders();
            out.putAll(response.getHeaders());
            out.set("X-Request-Id", requestId);

            return new ResponseEntity<>(response.getBody(), out, response.getStatusCode());

        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            String upstreamBody = e.getResponseBodyAsString();
            String path = requestPath(inboundRequest);

            log.warn("API2 {} {} -> {} (x-request-id={}) upstreamBody={}",
                    method, pathWithQuery, status, requestId, safe(upstreamBody));

            String bodyToReturn = normalizeUpstreamErrorBody(upstreamBody, status, path);

            HttpHeaders out = new HttpHeaders();
            out.setContentType(MediaType.APPLICATION_JSON);
            out.set("X-Request-Id", requestId);

            return ResponseEntity.status(status).headers(out).body(bodyToReturn);

        } catch (ResourceAccessException e) {
            String path = requestPath(inboundRequest);

            log.error("API2 unreachable for {} {} (x-request-id={}): {}",
                    method, pathWithQuery, requestId, e.getMessage());

            ApiError apiError = new ApiError(
                    Instant.now(),
                    HttpStatus.BAD_GATEWAY.value(),
                    "Bad Gateway",
                    "API2 is unavailable (connection/timeout)",
                    path,
                    null
            );

            String json = write(apiError);

            HttpHeaders out = new HttpHeaders();
            out.setContentType(MediaType.APPLICATION_JSON);
            out.set("X-Request-Id", requestId);

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).headers(out).body(json);
        }
    }

    private String normalizeUpstreamErrorBody(String upstreamBody, int status, String path) {
        if (upstreamBody == null || upstreamBody.isBlank()) {
            ApiError apiError = new ApiError(
                    Instant.now(),
                    status,
                    HttpStatus.valueOf(status).getReasonPhrase(),
                    "Upstream API2 returned an empty error body",
                    path,
                    null
            );
            return write(apiError);
        }

        try {
            JsonNode node = objectMapper.readTree(upstreamBody);

            if (looksLikeApiError(node)) {
                return upstreamBody;
            }

            ApiError apiError = new ApiError(
                    Instant.now(),
                    status,
                    HttpStatus.valueOf(status).getReasonPhrase(),
                    "Upstream API2 error",
                    path,
                    null
            );

            return write(apiError);

        } catch (Exception ex) {

            ApiError apiError = new ApiError(
                    Instant.now(),
                    status,
                    HttpStatus.valueOf(status).getReasonPhrase(),
                    safe(upstreamBody),
                    path,
                    null
            );
            return write(apiError);
        }
    }

    private boolean looksLikeApiError(JsonNode node) {

        return node.has("status") && node.has("message") && node.has("path");
    }

    private String requestPath(HttpServletRequest request) {
        if (request == null) return "/api/orders/**";
        return request.getRequestURI();
    }

    private String write(ApiError apiError) {
        try {
            return objectMapper.writeValueAsString(apiError);
        } catch (IOException e) {

            return "{\"status\":" + apiError.getStatus() + ",\"message\":\"" + safe(apiError.getMessage()) + "\"}";
        }
    }

    private String safe(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() > 600) return t.substring(0, 600) + "...(truncated)";
        return t.replace("\n", " ").replace("\r", " ");
    }
}
