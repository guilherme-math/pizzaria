package com.umc.pediupatrao.entity;

import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "auditorias")
public class Auditoria {
    @Id private String id;
    private String usuario;
    private String perfil;
    private Instant dataHora;
    private String operacao;
    private String entidade;
    private String entidadeId;
    private Map<String, Mudanca> campos;
    private String justificativa;
    public record Mudanca(Object anterior, Object novo) { }
}
