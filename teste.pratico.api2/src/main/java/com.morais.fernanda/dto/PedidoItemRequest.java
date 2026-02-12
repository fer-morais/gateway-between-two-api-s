package com.morais.fernanda.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class PedidoItemRequest {

    public String getProductName() {
        return productName;
    }

    @NotBlank(message = "productName é obrigatório")
    private String productName;

    @NotNull(message = "quantity é obrigatório")
    @Min(value = 1, message = "quantity deve ser no mínimo 1")
    private Integer quantity;

    @NotNull(message = "unitPrice é obrigatório")
    @Positive(message = "unitPrice deve ser maior que 0")
    private BigDecimal unitPrice;

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }


}
