package com.morais.fernanda.controller;

import com.morais.fernanda.client.Api2Client;
import com.morais.fernanda.dto.PedidoItemRequest;
import com.morais.fernanda.dto.PedidoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Orders (Gateway)", description = "Gateway que encaminha requisições para a API2 interna")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/orders")
public class OrdersProxyController {

    private final Api2Client api2Client;

    public OrdersProxyController(Api2Client api2Client) {
        this.api2Client = api2Client;
    }

    private String bearer(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }

  /*  @Operation(summary = "Listar todos os pedidos (com paginação)")
    @GetMapping
    public ResponseEntity<String> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @Parameter(hidden = true) HttpServletRequest request) {

        String path = "/api/orders?page=" + page + "&size=" + size;
        return api2Client.forward(HttpMethod.GET, path, bearer(request), null, request);
    }
*/
    @Operation(summary = "Listar todos os pedidos (com paginação)")
    @GetMapping
    public ResponseEntity<String> list(@ParameterObject Pageable pageable,
                                       @Parameter(hidden = true) HttpServletRequest request) {
        String path = "/api/orders";
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            path += "?" + query;
        }
        return api2Client.forward(HttpMethod.GET, path, bearer(request), null, request);
    }

    @Operation(summary = "Buscar pedido por ID")
    @GetMapping("/{id}")
    public ResponseEntity<String> getById(@PathVariable Long id, HttpServletRequest request) {
        return api2Client.forward(HttpMethod.GET, "/api/orders/" + id, bearer(request), null, request);
    }

    @Operation(summary = "Criar novo pedido")
    @PostMapping
    public ResponseEntity<String> create(@Valid @RequestBody PedidoRequest body, HttpServletRequest request) {
        return api2Client.forward(HttpMethod.POST, "/api/orders", bearer(request), body, request);
    }

    @Operation(summary = "Atualizar pedido")
    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id,
                                         @Valid @RequestBody PedidoRequest body,
                                         HttpServletRequest request) {
        return api2Client.forward(HttpMethod.PUT, "/api/orders/" + id, bearer(request), body, request);
    }

    @Operation(summary = "Deletar pedido")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id, HttpServletRequest request) {
        return api2Client.forward(HttpMethod.DELETE, "/api/orders/" + id, bearer(request), null, request);
    }

    @Operation(summary = "Listar itens de um pedido")
    @GetMapping("/{id}/items")
    public ResponseEntity<String> listItems(@PathVariable Long id, HttpServletRequest request) {
        return api2Client.forward(HttpMethod.GET, "/api/orders/" + id + "/items", bearer(request), null, request);
    }

    @Operation(summary = "Adicionar item ao pedido")
    @PostMapping("/{id}/items")
    public ResponseEntity<String> addItem(@PathVariable Long id,
                                          @Valid @RequestBody PedidoItemRequest body,
                                          HttpServletRequest request) {
        return api2Client.forward(HttpMethod.POST, "/api/orders/" + id + "/items", bearer(request), body, request);
    }
}
