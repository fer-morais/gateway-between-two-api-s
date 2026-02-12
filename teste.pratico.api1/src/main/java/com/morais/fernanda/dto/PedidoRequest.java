package com.morais.fernanda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PedidoRequest {

    @NotBlank(message = "customerName é obrigatório")
    private String customerName;

    @NotBlank(message = "customerEmail é obrigatório")
    @Email(message = "customerEmail deve ser um e-mail válido")
    private String customerEmail;

    @NotNull(message = "status é obrigatório")
    private String status;

    @Valid
    @Size(min = 1, message = "items deve ter pelo menos 1 item")
    private List<PedidoItemRequest> items;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<PedidoItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PedidoItemRequest> items) {
        this.items = items;
    }

}
