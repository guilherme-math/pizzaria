package com.umc.pediupatrao.entity;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "pedidos")
public class Pedido {
    @Id private String id;
    @Version private Long versao;
    private String clienteId;
    private String clienteNome;
    private String status;
    private List<ItemPedido> itens = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal percentualDesconto = BigDecimal.ZERO;
    private BigDecimal valorDesconto = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private String responsavelEntrada;
    private Instant entrada;
    private String responsavelSaida;
    private Instant saida;
    private String justificativaCancelamento;
    // Compatibilidade de leitura com pedidos anteriores a esta atividade.
    private String tipoPizza;
    private int quantidade;
    private String sabor;
}
