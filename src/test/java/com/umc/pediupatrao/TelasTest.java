package com.umc.pediupatrao;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.controller.*;
import com.umc.pediupatrao.service.*;
import com.umc.pediupatrao.entity.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest({HomeController.class, AuditoriaController.class})
@Import(SecurityConfig.class)
class TelasTest {
    @Autowired MockMvc mvc;
    @MockitoBean UsuarioDetailsService details;
    @MockitoBean UsuarioService usuarios;
    @MockitoBean ClienteService clientes;
    @MockitoBean ProdutoService produtos;
    @MockitoBean PedidoService pedidos;
    @MockitoBean AuditoriaService auditoria;
    @BeforeEach void setup() {
        when(details.loadUserByUsername(anyString())).thenAnswer(inv ->
            (org.springframework.security.core.userdetails.UserDetails) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
    @Test void loginTemTokenCsrf() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(containsString("name=\"_csrf\"")));
    }
    @Test void telasPrincipaisRenderizamParaCadaPerfil() throws Exception {
        for (String role : List.of("ADMIN","GERENTE","ATENDENTE")) {
            mvc.perform(get("/").with(user("u").roles(role))).andExpect(status().isOk());
            mvc.perform(get("/pedidos").with(user("u").roles(role))).andExpect(status().isOk()).andExpect(content().string(containsString("/js/api.js")));
            mvc.perform(get("/produtos").with(user("u").roles(role))).andExpect(status().isOk());
        }
    }
    @Test void formularioUsuarioNaoExpoeHash() throws Exception {
        Usuario u = new Usuario(); u.setId("u1"); u.setUsername("operador"); u.setRole("ATENDENTE"); u.setPassword("hash-nao-expor");
        when(usuarios.buscarPorId("u1")).thenReturn(Optional.of(u));
        mvc.perform(get("/usuarios/editar/u1").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("hash-nao-expor"))))
            .andExpect(content().string(containsString("GERENTE")));
    }
    @Test void auditoriaRenderizaValores() throws Exception {
        Auditoria evento = new Auditoria(); evento.setUsuario("atendente"); evento.setPerfil("ATENDENTE"); evento.setEntidade("Cliente"); evento.setEntidadeId("c1");
        evento.setOperacao("ALTERACAO_CLIENTE"); evento.setDataHora(java.time.Instant.now());
        evento.setCampos(Map.of("telefone", new Auditoria.Mudanca("1111", "2222")));
        when(auditoria.listar(0)).thenReturn(new PageImpl<>(List.of(evento)));
        mvc.perform(get("/auditoria").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
            .andExpect(content().string(containsString("1111"))).andExpect(content().string(containsString("2222")));
    }
    @Test void clientePodeSerConsultadoEEditadoPeloAtendente() throws Exception {
        mvc.perform(get("/clientes").with(user("a").roles("ATENDENTE"))).andExpect(status().isOk());
        mvc.perform(get("/clientes/novo").with(user("a").roles("ATENDENTE"))).andExpect(status().isOk());
    }
}
