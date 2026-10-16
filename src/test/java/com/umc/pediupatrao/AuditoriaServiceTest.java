package com.umc.pediupatrao;

import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.*;
import com.umc.pediupatrao.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AuditoriaServiceTest {
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }
    @Test void guardaTelefoneAnteriorENovoComIdentidade() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        AuditoriaService service = new AuditoriaService(mongo, new ObjectMapper().findAndRegisterModules(), mock(AuditoriaRepository.class));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("atendente", "", List.of(new SimpleGrantedAuthority("ROLE_ATENDENTE"))));
        Cliente cliente = new Cliente(); cliente.setId("c1"); cliente.setTelefone("1111");
        var antes = service.snapshot(cliente); cliente.setTelefone("2222");
        service.registrar("ALTERACAO_CLIENTE", "Cliente", "c1", antes, cliente, null);
        var captor = ArgumentCaptor.forClass(Auditoria.class); verify(mongo).insert(captor.capture());
        Auditoria a = captor.getValue(); assertEquals("1111", a.getCampos().get("telefone").anterior());
        assertEquals("2222", a.getCampos().get("telefone").novo()); assertEquals("atendente", a.getUsuario());
        assertEquals("ATENDENTE", a.getPerfil()); assertNotNull(a.getDataHora());
        assertFalse(a.getCampos().containsKey("versao"));
    }
    @Test void naoAceitaSnapshotDeCredenciais() {
        AuditoriaService service = new AuditoriaService(mock(MongoTemplate.class), new ObjectMapper(), mock(AuditoriaRepository.class));
        assertThrows(IllegalArgumentException.class, () -> service.snapshot(new Usuario()));
    }
    @Test void senhaNuncaSaiNoJson() throws Exception {
        Usuario u = new Usuario(); u.setPassword("hash-secreto"); u.setUsername("teste");
        ObjectMapper mapper = new ObjectMapper(); String json = mapper.writeValueAsString(u);
        assertFalse(json.contains("hash-secreto")); assertFalse(json.contains("password"));
        assertEquals("nova-senha", mapper.readValue("{\"password\":\"nova-senha\"}", Usuario.class).getPassword());
    }
}
