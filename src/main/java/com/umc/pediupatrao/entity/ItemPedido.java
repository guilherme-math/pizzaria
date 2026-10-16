package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
public record ItemPedido(String produtoId, String nome, int quantidade, BigDecimal precoUnitario) {
    public BigDecimal getSubtotal() { return precoUnitario.multiply(BigDecimal.valueOf(quantidade)); }
}
