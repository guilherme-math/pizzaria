package com.umc.pediupatrao.dashboard;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface CustoDiarioRepository extends MongoRepository<CustoDiario, String> {
    @org.springframework.data.mongodb.repository.Query(value = "{ '_id': { '$gte': ?0, '$lte': ?1 } }", sort = "{ '_id': 1 }")
    List<CustoDiario> findByIdGreaterThanEqualAndIdLessThanEqualOrderById(String inicio, String fim);
}
