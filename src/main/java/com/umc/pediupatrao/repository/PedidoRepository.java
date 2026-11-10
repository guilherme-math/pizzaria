package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Pedido;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
    @org.springframework.data.mongodb.repository.Query("{ 'entrada': { '$gte': ?0, '$lt': ?1 } }")
    List<Pedido> findByEntradaGreaterThanEqualAndEntradaLessThan(java.time.Instant inicio, java.time.Instant fim);
    long countByEntradaIsNull();
    boolean existsByClienteId(String clienteId);
    List<Pedido> findByClienteId(String clienteId);
}
