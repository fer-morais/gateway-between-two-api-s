package com.morais.fernanda.service;

import com.morais.fernanda.dto.PedidoItemRequest;
import com.morais.fernanda.dto.PedidoRequest;
import com.morais.fernanda.exception.ResourceNotFoundException;
import com.morais.fernanda.model.Pedido;
import com.morais.fernanda.model.PedidoItem;
import com.morais.fernanda.model.PedidoStatus;
import com.morais.fernanda.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PedidoServiceTest {

    private PedidoRepository pedidoRepository;
    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        pedidoRepository = mock(PedidoRepository.class);
        pedidoService = new PedidoService(pedidoRepository);
    }

    @Test
    void getById_quandoExiste_deveRetornarPedido() {
        Pedido p = new Pedido();
        p.setId(10L);

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(p));

        Pedido result = pedidoService.getById(10L);

        assertEquals(10L, result.getId());
        verify(pedidoRepository).findById(10L);
    }

    @Test
    void getById_quandoNaoExiste_deveLancarNotFound() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pedidoService.getById(99L));
        verify(pedidoRepository).findById(99L);
        verifyNoMoreInteractions(pedidoRepository);
    }

    @Test
    void create_quandoStatusNulo_deveSetarPending_calcularTotais_eSalvar() {

        PedidoRequest req = mock(PedidoRequest.class);
        when(req.getCustomerName()).thenReturn("Fernanda");
        when(req.getCustomerEmail()).thenReturn("fer@teste.com");
        when(req.getStatus()).thenReturn(null);

        PedidoItemRequest item1 = mock(PedidoItemRequest.class);
        when(item1.getProductName()).thenReturn("Produto A");
        when(item1.getQuantity()).thenReturn(2);
        when(item1.getUnitPrice()).thenReturn(new BigDecimal("10.00"));

        PedidoItemRequest item2 = mock(PedidoItemRequest.class);
        when(item2.getProductName()).thenReturn("Produto B");
        when(item2.getQuantity()).thenReturn(1);
        when(item2.getUnitPrice()).thenReturn(new BigDecimal("5.50"));

        when(req.getItems()).thenReturn(List.of(item1, item2));

        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido saved = pedidoService.create(req);

        assertNotNull(saved.getOrderDate(), "orderDate deve ser preenchida no create");
        assertEquals(PedidoStatus.PENDING, saved.getStatus(), "status default deve ser PENDING");

        assertNotNull(saved.getItems());
        assertEquals(2, saved.getItems().size());

        assertEquals(0, new BigDecimal("25.50").compareTo(saved.getTotalAmount()));

        PedidoItem i1 = saved.getItems().get(0);
        assertEquals("Produto A", i1.getProductName());
        assertEquals(2, i1.getQuantity());
        assertEquals(0, new BigDecimal("10.00").compareTo(i1.getUnitPrice()));
        assertEquals(0, new BigDecimal("20.00").compareTo(i1.getSubtotal()));
        assertSame(saved, i1.getPedido(), "item deve apontar pro pedido");

        PedidoItem i2 = saved.getItems().get(1);
        assertEquals(0, new BigDecimal("5.50").compareTo(i2.getSubtotal()));

        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    void create_quandoStatusInformado_deveConverterParaEnum() {
        PedidoRequest req = mock(PedidoRequest.class);
        when(req.getCustomerName()).thenReturn("A");
        when(req.getCustomerEmail()).thenReturn("a@a.com");
        when(req.getStatus()).thenReturn("confirmed");
        when(req.getItems()).thenReturn(List.of());

        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido saved = pedidoService.create(req);

        assertEquals(PedidoStatus.CONFIRMED, saved.getStatus());
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    void deleteById_quandoExiste_deveDeletar() {
        Pedido p = new Pedido();
        p.setId(1L);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(p));

        pedidoService.deleteById(1L);

        verify(pedidoRepository).findById(1L);
        verify(pedidoRepository).delete(p);
    }

    @Test
    void deleteById_quandoNaoExiste_deveLancarNotFound() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pedidoService.deleteById(1L));

        verify(pedidoRepository).findById(1L);
        verify(pedidoRepository, never()).delete(any());
    }

    @Test
    void update_quandoExiste_deveAtualizarCamposESalvar() {
        Pedido exist = new Pedido();
        exist.setId(7L);
        exist.setCustomerName("Old");
        exist.setCustomerEmail("old@a.com");
        exist.setStatus(PedidoStatus.PENDING);

        PedidoRequest req = mock(PedidoRequest.class);
        when(req.getCustomerName()).thenReturn("New");
        when(req.getCustomerEmail()).thenReturn("new@a.com");
        when(req.getStatus()).thenReturn("DELIVERED");

        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(exist));

        pedidoService.update(req, 7L);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());

        Pedido saved = captor.getValue();
        assertEquals("New", saved.getCustomerName());
        assertEquals("new@a.com", saved.getCustomerEmail());
        assertEquals(PedidoStatus.DELIVERED, saved.getStatus());
    }

    @Test
    void listById_quandoNaoExiste_deveLancarNotFound() {
        when(pedidoRepository.findById(55L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pedidoService.listById(55L));
    }

    @Test
    void listById_quandoItemsNulo_deveRetornarListaVazia() {
        Pedido p = new Pedido();
        p.setId(2L);
        p.setItems(null);

        when(pedidoRepository.findById(2L)).thenReturn(Optional.of(p));

        List<PedidoItem> items = pedidoService.listById(2L);

        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    void addByItem_deveAdicionarItem_recalcularTotal_eSalvar() {
        Pedido pedido = new Pedido();
        pedido.setId(3L);

        PedidoItem existente = new PedidoItem();
        existente.setProductName("X");
        existente.setQuantity(1);
        existente.setUnitPrice(new BigDecimal("10.00"));
        existente.setSubtotal(new BigDecimal("10.00"));
        existente.setPedido(pedido);

        pedido.setItems(new java.util.ArrayList<>(List.of(existente)));
        pedido.setTotalAmount(new BigDecimal("10.00"));

        when(pedidoRepository.findById(3L)).thenReturn(Optional.of(pedido));

        PedidoItemRequest req = mock(PedidoItemRequest.class);
        when(req.getProductName()).thenReturn("Novo");
        when(req.getQuantity()).thenReturn(2);
        when(req.getUnitPrice()).thenReturn(new BigDecimal("7.50"));

        pedidoService.addByItem(3L, req);

        assertEquals(0, new BigDecimal("25.00").compareTo(pedido.getTotalAmount()));
        assertEquals(2, pedido.getItems().size());

        PedidoItem added = pedido.getItems().get(1);
        assertEquals("Novo", added.getProductName());
        assertEquals(0, new BigDecimal("15.00").compareTo(added.getSubtotal()));
        assertSame(pedido, added.getPedido());

        verify(pedidoRepository).save(pedido);
    }
}
