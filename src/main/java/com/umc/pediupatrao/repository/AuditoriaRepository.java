package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Auditoria;
import org.springframework.data.repository.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Contrato deliberadamente somente de leitura; insercao pelo servico de auditoria.
public interface AuditoriaRepository extends Repository<Auditoria, String> {
    Page<Auditoria> findAll(Pageable pageable);
}
