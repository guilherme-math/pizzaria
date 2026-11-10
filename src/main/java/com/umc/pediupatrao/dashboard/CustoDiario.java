package com.umc.pediupatrao.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "custos_diarios")
public class CustoDiario {
    @Id private String id;
    @Version private Long versao;
    private BigDecimal valor;
    private String observacao;
    private String atualizadoPor;
    private Instant atualizadoEm;
}
