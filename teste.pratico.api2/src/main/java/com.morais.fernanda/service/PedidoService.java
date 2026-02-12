package com.morais.fernanda.service;

import com.morais.fernanda.dto.PedidoItemRequest;
import com.morais.fernanda.dto.PedidoRequest;
import com.morais.fernanda.exception.ResourceNotFoundException;
import com.morais.fernanda.model.Pedido;
import com.morais.fernanda.model.PedidoItem;
import com.morais.fernanda.model.PedidoStatus;
import com.morais.fernanda.repository.PedidoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public Page<Pedido> list(Pageable pageable) {
        return pedidoRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Pedido getById(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com id: " + id));
    }

    @Transactional
    public Pedido create(PedidoRequest request) {
        Pedido pedido = new Pedido();
        pedido.setCustomerName(request.getCustomerName());
        pedido.setCustomerEmail(request.getCustomerEmail());
        pedido.setOrderDate(LocalDateTime.now());

        if (request.getStatus() != null) {
            pedido.setStatus(PedidoStatus.valueOf(request.getStatus().toUpperCase()));
        } else {
            pedido.setStatus(PedidoStatus.PENDING);
        }

        if (pedido.getItems() == null) {
            pedido.setItems(new ArrayList<>());
        }

        BigDecimal total = BigDecimal.ZERO;

        if (request.getItems() != null) {
            for (PedidoItemRequest itemReq : request.getItems()) {
                PedidoItem item = new PedidoItem();
                item.setProductName(itemReq.getProductName());
                item.setQuantity(itemReq.getQuantity());
                item.setUnitPrice(itemReq.getUnitPrice());

                BigDecimal subtotal = itemReq.getUnitPrice()
                        .multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                item.setSubtotal(subtotal);

                item.setPedido(pedido);
                pedido.getItems().add(item);

                total = total.add(subtotal);
            }
        }

        pedido.setTotalAmount(total);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public void deleteById(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com id: " + id));

        pedidoRepository.delete(pedido);
    }

    @Transactional
    public void update(PedidoRequest request, Long id) {
        Pedido exist = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com id: " + id));

        exist.setCustomerName(request.getCustomerName());
        exist.setCustomerEmail(request.getCustomerEmail());

        if (request.getStatus() != null) {
            exist.setStatus(PedidoStatus.valueOf(request.getStatus().toUpperCase()));
        }

        pedidoRepository.save(exist);
    }

    @Transactional(readOnly = true)
    public List<PedidoItem> listById(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com id: " + id));

        return pedido.getItems() == null ? List.of() : pedido.getItems();
    }

    @Transactional
    public void addByItem(Long id, PedidoItemRequest request) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com id: " + id));

        if (pedido.getItems() == null) {
            pedido.setItems(new ArrayList<>());
        }

        PedidoItem item = new PedidoItem();
        item.setProductName(request.getProductName());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());

        BigDecimal subtotal = request.getUnitPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));
        item.setSubtotal(subtotal);

        item.setPedido(pedido);
        pedido.getItems().add(item);

        BigDecimal totalRecalculado = pedido.getItems().stream()
                .map(PedidoItem::getSubtotal)
                .filter(s -> s != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        pedido.setTotalAmount(totalRecalculado);

        pedidoRepository.save(pedido);
    }
}
