package com.morais.fernanda.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class Api2ClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void forward_quando200_deveRepassarBodyHeadersERequestId() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        String baseUrl = "http://localhost:8081";
        Api2Client client = new Api2Client(restTemplate, objectMapper, baseUrl);

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        String requestId = UUID.randomUUID().toString();
        when(req.getHeader("X-Request-Id")).thenReturn(requestId);
        when(req.getRequestURI()).thenReturn("/api/orders");

        String path = "/api/orders?page=0&size=10";
        String url = baseUrl + path;

        server.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Request-Id", requestId))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer ABC"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON)
                        .header("X-Upstream", "yes"));

        ResponseEntity<String> out = client.forward(
                HttpMethod.GET,
                path,
                "Bearer ABC",
                null,
                req
        );

        server.verify();

        assertEquals(200, out.getStatusCode().value());
        assertEquals("{\"ok\":true}", out.getBody());

        assertEquals("yes", out.getHeaders().getFirst("X-Upstream"));
        assertEquals(requestId, out.getHeaders().getFirst("X-Request-Id"));
    }

    @Test
    void forward_quandoHttpError_eBodyJaEhApiError_deveRepassarMesmoBody() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        String baseUrl = "http://localhost:8081";
        Api2Client client = new Api2Client(restTemplate, objectMapper, baseUrl);

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getHeader("X-Request-Id")).thenReturn("REQ-1");
        when(req.getRequestURI()).thenReturn("/api/orders/999");

        String path = "/api/orders/999";
        String url = baseUrl + path;

        String apiErrorJson = """
                {
                  "timestamp":"2026-02-18T00:00:00Z",
                  "status":404,
                  "error":"Not Found",
                  "message":"Pedido não encontrado",
                  "path":"/api/orders/999",
                  "errors":null
                }
                """;

        server.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(apiErrorJson));

        ResponseEntity<String> out = client.forward(
                HttpMethod.GET,
                path,
                "Bearer OK",
                null,
                req
        );

        server.verify();

        assertEquals(404, out.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_JSON, out.getHeaders().getContentType());
        assertEquals("REQ-1", out.getHeaders().getFirst("X-Request-Id"));

        assertNotNull(out.getBody());
        assertTrue(out.getBody().contains("\"status\":404"));
        assertTrue(out.getBody().contains("\"message\":\"Pedido não encontrado\""));
        assertTrue(out.getBody().contains("\"path\":\"/api/orders/999\""));
    }

    @Test
    void forward_quandoHttpError_eJsonNaoEhApiError_deveNormalizarParaApiError() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        String baseUrl = "http://localhost:8081";
        Api2Client client = new Api2Client(restTemplate, objectMapper, baseUrl);

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getHeader("X-Request-Id")).thenReturn("REQ-2");
        when(req.getRequestURI()).thenReturn("/api/orders");

        String path = "/api/orders";
        String url = baseUrl + path;

        String upstreamJsonNaoPadrao = """
                { "errorCode": "X1", "details": "qualquer coisa" }
                """;

        server.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(upstreamJsonNaoPadrao));

        ResponseEntity<String> out = client.forward(
                HttpMethod.GET, path, "Bearer OK", null, req
        );

        server.verify();

        assertEquals(400, out.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_JSON, out.getHeaders().getContentType());
        assertEquals("REQ-2", out.getHeaders().getFirst("X-Request-Id"));

        var json = objectMapper.readTree(out.getBody());
        assertEquals(400, json.path("status").asInt());
        assertEquals("Upstream API2 error", json.path("message").asText());
        assertEquals("/api/orders", json.path("path").asText());

        String error = json.path("error").asText();
        if (!error.isBlank()) {
            assertEquals("Bad Request", error);
        }
    }

    @Test
    void forward_quandoHttpError_eBodyVazio_deveNormalizarParaApiError() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        String baseUrl = "http://localhost:8081";
        Api2Client client = new Api2Client(restTemplate, objectMapper, baseUrl);

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getHeader("X-Request-Id")).thenReturn("REQ-3");
        when(req.getRequestURI()).thenReturn("/api/orders");

        String path = "/api/orders";
        String url = baseUrl + path;

        server.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(""));

        ResponseEntity<String> out = client.forward(
                HttpMethod.GET, path, "Bearer OK", null, req
        );

        server.verify();

        assertEquals(500, out.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_JSON, out.getHeaders().getContentType());
        assertEquals("REQ-3", out.getHeaders().getFirst("X-Request-Id"));

        var json = objectMapper.readTree(out.getBody());
        assertEquals(500, json.path("status").asInt());
        assertEquals("Upstream API2 returned an empty error body", json.path("message").asText());
        assertEquals("/api/orders", json.path("path").asText());

        String error = json.path("error").asText();
        if (!error.isBlank()) {
            assertEquals("Internal Server Error", error);
        }
    }

    @Test
    void forward_quandoApi2Indisponivel_deveRetornar502ComApiError() throws Exception {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

        String baseUrl = "http://localhost:8081";
        Api2Client client = new Api2Client(restTemplate, objectMapper, baseUrl);

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getHeader("X-Request-Id")).thenReturn("REQ-4");
        when(req.getRequestURI()).thenReturn("/api/orders");

        String path = "/api/orders";
        String url = baseUrl + path;

        when(restTemplate.exchange(
                Mockito.eq(url),
                Mockito.eq(HttpMethod.GET),
                Mockito.any(HttpEntity.class),
                Mockito.eq(String.class)
        )).thenThrow(new ResourceAccessException("timeout"));

        ResponseEntity<String> out = client.forward(
                HttpMethod.GET, path, "Bearer OK", null, req
        );

        assertEquals(502, out.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_JSON, out.getHeaders().getContentType());
        assertEquals("REQ-4", out.getHeaders().getFirst("X-Request-Id"));

        var json = objectMapper.readTree(out.getBody());
        assertEquals(502, json.path("status").asInt());
        assertEquals("API2 is unavailable (connection/timeout)", json.path("message").asText());
        assertEquals("/api/orders", json.path("path").asText());

        String error = json.path("error").asText();
        if (!error.isBlank()) {
            assertEquals("Bad Gateway", error);
        }
    }
}
