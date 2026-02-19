package com.morais.fernanda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.morais.fernanda.dto.PedidoItemRequest;
import com.morais.fernanda.dto.PedidoRequest;
import com.morais.fernanda.exception.ResourceNotFoundException;
import com.morais.fernanda.model.Pedido;
import com.morais.fernanda.model.PedidoItem;
import com.morais.fernanda.model.PedidoStatus;
import com.morais.fernanda.security.JwtAuthFilter;
import com.morais.fernanda.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PedidoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
class PedidoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean PedidoService pedidoService;

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper;
        }
    }

    private PedidoRequest pedidoRequestValido() {
        PedidoItemRequest item = new PedidoItemRequest();
        item.setProductName("Produto A");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("10.00"));

        PedidoRequest req = new PedidoRequest();
        req.setCustomerName("Fernanda");
        req.setCustomerEmail("fer@teste.com");
        req.setStatus("PENDING");
        req.setItems(List.of(item));
        return req;
    }

    private Pedido pedidoRetorno(Long id) {
        Pedido p = new Pedido();
        p.setId(id);
        p.setCustomerName("Fernanda");
        p.setCustomerEmail("fer@teste.com");
        p.setOrderDate(LocalDateTime.now());
        p.setStatus(PedidoStatus.PENDING);
        p.setTotalAmount(new BigDecimal("20.00"));

        PedidoItem it = new PedidoItem();
        it.setId(1L);
        it.setProductName("Produto A");
        it.setQuantity(2);
        it.setUnitPrice(new BigDecimal("10.00"));
        it.setSubtotal(new BigDecimal("20.00"));
        it.setPedido(p);

        p.setItems(List.of(it));
        return p;
    }

    @Test
    void listar_deveRetornar200ComPagina() throws Exception {
        Pedido p1 = pedidoRetorno(1L);
        Pedido p2 = pedidoRetorno(2L);

        when(pedidoService.list(any())).thenReturn(
                new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 20), 2)
        );

        mockMvc.perform(get("/api/orders?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[1].id", is(2)));
    }

    @Test
    void adicionar_deveRetornar201ELocation() throws Exception {
        PedidoRequest req = pedidoRequestValido();
        Pedido saved = pedidoRetorno(10L);

        when(pedidoService.create(any(PedidoRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/10"))
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.customerName", is("Fernanda")))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void adicionar_quandoDtoInvalido_deveRetornar400() throws Exception {
        PedidoRequest req = new PedidoRequest();
        req.setCustomerName(" ");
        req.setCustomerEmail("email_invalido");
        req.setStatus(null);
        req.setItems(List.of());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarId_deveRetornar200() throws Exception {
        when(pedidoService.getById(5L)).thenReturn(pedidoRetorno(5L));

        mockMvc.perform(get("/api/orders/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void listarId_quandoNaoEncontrado_deveRetornar404() throws Exception {
        when(pedidoService.getById(999L)).thenThrow(new ResourceNotFoundException("Pedido não encontrado"));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletar_deveRetornar204() throws Exception {
        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletar_quandoNaoEncontrado_deveRetornar404() throws Exception {
        doThrow(new ResourceNotFoundException("Pedido não encontrado"))
                .when(pedidoService).deleteById(123L);

        mockMvc.perform(delete("/api/orders/123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizar_deveRetornar204() throws Exception {
        PedidoRequest req = pedidoRequestValido();

        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    void atualizar_quandoDtoInvalido_deveRetornar400() throws Exception {
        PedidoRequest req = new PedidoRequest();

        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizar_quandoNaoEncontrado_deveRetornar404() throws Exception {
        PedidoRequest req = pedidoRequestValido();

        doThrow(new ResourceNotFoundException("Pedido não encontrado"))
                .when(pedidoService).update(any(PedidoRequest.class), eq(999L));

        mockMvc.perform(put("/api/orders/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarItens_deveRetornar200() throws Exception {
        PedidoItem it = new PedidoItem();
        it.setId(1L);
        it.setProductName("Produto A");
        it.setQuantity(2);
        it.setUnitPrice(new BigDecimal("10.00"));
        it.setSubtotal(new BigDecimal("20.00"));
        it.setPedido(new Pedido());

        when(pedidoService.listById(1L)).thenReturn(List.of(it));

        mockMvc.perform(get("/api/orders/1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productName", is("Produto A")));
    }

    @Test
    void listarItens_quandoNaoEncontrado_deveRetornar404() throws Exception {
        when(pedidoService.listById(777L)).thenThrow(new ResourceNotFoundException("Pedido não encontrado"));

        mockMvc.perform(get("/api/orders/777/items"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adicionarItem_deveRetornar204() throws Exception {
        PedidoItemRequest req = new PedidoItemRequest();
        req.setProductName("Novo");
        req.setQuantity(1);
        req.setUnitPrice(new BigDecimal("9.99"));

        mockMvc.perform(post("/api/orders/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    void adicionarItem_quandoDtoInvalido_deveRetornar400() throws Exception {
        PedidoItemRequest req = new PedidoItemRequest();
        req.setProductName("");
        req.setQuantity(0);
        req.setUnitPrice(new BigDecimal("-1"));

        mockMvc.perform(post("/api/orders/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adicionarItem_quandoNaoEncontrado_deveRetornar404() throws Exception {
        PedidoItemRequest req = new PedidoItemRequest();
        req.setProductName("Novo");
        req.setQuantity(1);
        req.setUnitPrice(new BigDecimal("9.99"));

        doThrow(new ResourceNotFoundException("Pedido não encontrado"))
                .when(pedidoService).addByItem(eq(999L), any(PedidoItemRequest.class));

        mockMvc.perform(post("/api/orders/999/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
