package com.morais.fernanda.controller;

import com.morais.fernanda.client.Api2Client;
import com.morais.fernanda.dto.PedidoItemRequest;
import com.morais.fernanda.dto.PedidoRequest;
import com.morais.fernanda.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest
@TestPropertySource(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "security.jwt.expiration-minutes=60",
        "api2.base-url=http://localhost:8081"
})
class OrdersProxySecurityTest {

    @Autowired
    WebApplicationContext wac;

    @Autowired
    JwtService jwtService;

    @MockitoBean
    Api2Client api2Client;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getOrders_semToken_deveRetornar401_comApiError() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Token ausente ou inválido"));

        verifyNoInteractions(api2Client);
    }

    @Test
    void getOrders_tokenInvalido_deveRetornar401_comApiError() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token_invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Token inválido ou expirado"));

        verifyNoInteractions(api2Client);
    }

    @Test
    void getOrders_tokenValido_deveRetornar200_eEncaminharParaApi2Client() throws Exception {
        String token = jwtService.generateToken("usuario");

        when(api2Client.forward(eq(HttpMethod.GET), anyString(), anyString(), isNull(), any()))
                .thenReturn(ResponseEntity.ok("OK_FROM_API2"));

        mockMvc.perform(get("/api/orders?page=0&size=10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("OK_FROM_API2"));

        verify(api2Client, times(1)).forward(
                eq(HttpMethod.GET),
                eq("/api/orders?page=0&size=10"),
                eq("Bearer " + token),
                isNull(),
                any()
        );
    }

    @Test
    void postOrders_tokenValido_deveEncaminharBodyCorretamente() throws Exception {
        String token = jwtService.generateToken("usuario");

        when(api2Client.forward(eq(HttpMethod.POST), eq("/api/orders"), anyString(), any(), any()))
                .thenReturn(ResponseEntity.status(201).body("{\"id\":10}"));

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Fernanda",
                                  "customerEmail": "fer@teste.com",
                                  "status": "PENDING",
                                  "items": [
                                    { "productName": "Produto A", "quantity": 2, "unitPrice": 10.00 }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string("{\"id\":10}"));

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);

        verify(api2Client, times(1)).forward(
                eq(HttpMethod.POST),
                eq("/api/orders"),
                eq("Bearer " + token),
                bodyCaptor.capture(),
                any()
        );

        Object bodySent = bodyCaptor.getValue();
        assertNotNull(bodySent);
        assertTrue(bodySent instanceof PedidoRequest);

        PedidoRequest sent = (PedidoRequest) bodySent;
        assertEquals("Fernanda", sent.getCustomerName());
        assertEquals("fer@teste.com", sent.getCustomerEmail());
        assertEquals("PENDING", sent.getStatus());
        assertNotNull(sent.getItems());
        assertEquals(1, sent.getItems().size());
        assertEquals("Produto A", sent.getItems().get(0).getProductName());
        assertEquals(2, sent.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("10.00"), sent.getItems().get(0).getUnitPrice());
    }
}
