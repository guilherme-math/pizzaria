package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {
    private final MongoTemplate mongo;
    private final ObjectMapper mapper;
    private final AuditoriaRepository repository;
    public AuditoriaService(MongoTemplate mongo, ObjectMapper mapper, AuditoriaRepository repository) {
        this.mongo = mongo; this.mapper = mapper; this.repository = repository;
    }
    public Map<String, Object> snapshot(Object entidade) {
        if (entidade == null) return Map.of();
        if (!(entidade instanceof Pedido) && !(entidade instanceof Cliente)) {
            throw new IllegalArgumentException("Entidade nao autorizada para auditoria");
        }
        Map<String, Object> dados = mapper.convertValue(entidade, new TypeReference<Map<String, Object>>() {});
        dados.remove("versao");
        return dados;
    }
    public void registrar(String operacao, String entidade, String id,
            Map<String, Object> anterior, Object depois, String justificativa) {
        Map<String, Object> novo = snapshot(depois);
        Set<String> chaves = new LinkedHashSet<>(anterior.keySet());
        chaves.addAll(novo.keySet());
        Map<String, Auditoria.Mudanca> campos = new LinkedHashMap<>();
        for (String chave : chaves) {
            if (!Objects.equals(anterior.get(chave), novo.get(chave))) {
                campos.put(chave, new Auditoria.Mudanca(anterior.get(chave), novo.get(chave)));
            }
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new IllegalStateException("Auditoria exige usuario autenticado");
        }
        Auditoria evento = new Auditoria();
        evento.setUsuario(auth.getName());
        evento.setPerfil(auth.getAuthorities().stream().map(a -> a.getAuthority().replace("ROLE_", "")).findFirst().orElseThrow());
        evento.setDataHora(Instant.now());
        evento.setOperacao(operacao); evento.setEntidade(entidade); evento.setEntidadeId(id);
        evento.setCampos(campos); evento.setJustificativa(justificativa);
        mongo.insert(evento);
    }
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Page<Auditoria> listar(int pagina) {
        return repository.findAll(PageRequest.of(Math.max(0, pagina), 30, Sort.by(Sort.Direction.DESC, "dataHora")));
    }
}
