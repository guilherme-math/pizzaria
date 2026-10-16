package com.umc.pediupatrao;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringJUnitWebConfig(SecurityAccessTest.Config.class)
class SecurityAccessTest {
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, Probe.class})
    static class Config { }

    @RestController
    static class Probe {
        @RequestMapping({"/api/usuarios", "/api/auditoria", "/api/clientes", "/api/pedidos", "/api/pedidos/1/status", "/api/pedidos/1/desconto", "/api/pedidos/1/cancelamento"})
        String ok() { return "ok"; }
    }

    @MockitoBean UsuarioDetailsService details;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    @BeforeEach void setup() {
        org.mockito.Mockito.when(details.loadUserByUsername(org.mockito.ArgumentMatchers.anyString())).thenAnswer(inv ->
            (org.springframework.security.core.userdetails.UserDetails) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        mvc = webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test void somenteAdminGerenciaUsuarios() throws Exception {
        mvc.perform(post("/api/usuarios").with(user("a").roles("ATENDENTE")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios").with(user("g").roles("GERENTE")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios").with(user("a").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
    }
    @Test void descontoECancelamentoExclusivosGerente() throws Exception {
        for (String operacao : new String[]{"desconto", "cancelamento"}) {
            for (String role : new String[]{"ADMIN", "ATENDENTE"}) {
                mvc.perform(post("/api/pedidos/1/" + operacao).with(user("u").roles(role)).with(csrf())).andExpect(status().isForbidden());
            }
            mvc.perform(post("/api/pedidos/1/" + operacao).with(user("g").roles("GERENTE")).with(csrf())).andExpect(status().isOk());
        }
    }
    @Test void perfilRevogadoValeNaSessaoExistente() throws Exception {
        org.mockito.Mockito.doReturn(
            org.springframework.security.core.userdetails.User.withUsername("admin-antigo").password("hash").roles("ATENDENTE").build()).when(details).loadUserByUsername("admin-antigo");
        mvc.perform(get("/api/usuarios").with(user("admin-antigo").roles("ADMIN"))).andExpect(status().isForbidden());
    }
    @Test void csrfObrigatorio() throws Exception {
        mvc.perform(post("/api/usuarios").with(user("a").roles("ADMIN"))).andExpect(status().isForbidden());
    }
    @Test void auditoriaRestrita() throws Exception {
        mvc.perform(get("/api/auditoria").with(user("a").roles("ATENDENTE"))).andExpect(status().isForbidden());
        for (String role : new String[]{"ADMIN", "GERENTE"}) {
            mvc.perform(get("/api/auditoria").with(user("u").roles(role))).andExpect(status().isOk());
        }
    }
    @Test void registroETransicaoSeparados() throws Exception {
        mvc.perform(post("/api/pedidos").with(user("a").roles("ATENDENTE")).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/pedidos").with(user("a").roles("ADMIN")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/api/pedidos/1/status").with(user("a").roles("ATENDENTE")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/api/pedidos/1/status").with(user("g").roles("GERENTE")).with(csrf())).andExpect(status().isOk());
    }
}


