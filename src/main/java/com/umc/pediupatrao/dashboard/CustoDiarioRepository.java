package com.umc.pediupatrao.dashboard;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface CustoDiarioRepository extends MongoRepository<CustoDiario, String> {
    List<CustoDiario> findByIdGreaterThanEqualAndIdLessThanEqualOrderById(String inicio, String fim);
}
