package com.morais.fernanda.controller;

import com.morais.fernanda.dto.PedidoItemRequest;
import com.morais.fernanda.dto.PedidoRequest;
import com.morais.fernanda.model.Pedido;
import com.morais.fernanda.model.PedidoItem;
import com.morais.fernanda.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public ResponseEntity<Page<Pedido>> listar(Pageable pageable) {
        return ResponseEntity.ok(pedidoService.list(pageable));
    }

    @PostMapping
    public ResponseEntity<Pedido> adicionar(@Valid @RequestBody PedidoRequest request) {
        Pedido criado = pedidoService.create(request);
        return ResponseEntity
                .created(URI.create("/api/orders/" + criado.getId()))
                .body(criado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> listarId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        pedidoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> atualizar(@Valid @RequestBody PedidoRequest request, @PathVariable Long id) {
        pedidoService.update(request, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<PedidoItem>> listarItens(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.listById(id));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<Void> adicionarItem(@PathVariable Long id, @Valid @RequestBody PedidoItemRequest request) {
        pedidoService.addByItem(id, request);
        return ResponseEntity.noContent().build();
    }
}
