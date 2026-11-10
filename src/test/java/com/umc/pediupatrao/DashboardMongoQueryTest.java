package com.umc.pediupatrao;

import com.umc.pediupatrao.dashboard.CustoDiario;
import com.umc.pediupatrao.dashboard.CustoDiarioRepository;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.time.Instant;
import java.util.Date;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DashboardMongoQueryTest {
    @Test void repositoriosMontamIntervalosSemRepetirCamposBson() {
        MongoMappingContext context = new MongoMappingContext();
        MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.afterPropertiesSet();
        context.setSimpleTypeHolder(converter.getCustomConversions().getSimpleTypeHolder());
        context.afterPropertiesSet();
        MongoOperations mongo = mock(MongoOperations.class, RETURNS_DEEP_STUBS);
        when(mongo.getConverter()).thenReturn(converter);
        when(mongo.execute(org.mockito.ArgumentMatchers.<org.springframework.data.mongodb.core.DbCallback<org.bson.codecs.configuration.CodecRegistry>>any()))
            .thenReturn(com.mongodb.MongoClientSettings.getDefaultCodecRegistry());
        var factory = new MongoRepositoryFactory(mongo);
        var pedidos = factory.getRepository(PedidoRepository.class);
        var custos = factory.getRepository(CustoDiarioRepository.class);
        var de = Instant.parse("2026-11-05T03:00:00Z");
        var ate = Instant.parse("2026-11-06T03:00:00Z");
        pedidos.findByEntradaGreaterThanEqualAndEntradaLessThan(de, ate);
        custos.findByIdGreaterThanEqualAndIdLessThanEqualOrderById("2026-11-05", "2026-11-06");

        var pedidoQuery = ArgumentCaptor.forClass(Query.class);
        verify(mongo.query(Pedido.class).as(Pedido.class)).matching(pedidoQuery.capture());
        assertEquals(new Document("entrada", new Document("$gte", Date.from(de)).append("$lt", Date.from(ate))),
            pedidoQuery.getValue().getQueryObject());
        var custoQuery = ArgumentCaptor.forClass(Query.class);
        verify(mongo.query(CustoDiario.class).as(CustoDiario.class)).matching(custoQuery.capture());
        assertEquals(new Document("_id", new Document("$gte", "2026-11-05").append("$lte", "2026-11-06")),
            custoQuery.getValue().getQueryObject());
        assertEquals(new Document("_id", 1), custoQuery.getValue().getSortObject());
    }
}
