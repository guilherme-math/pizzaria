package com.umc.pediupatrao;

import com.umc.pediupatrao.dashboard.*;
import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, DashboardService.class, DashboardConfig.class})
class DashboardApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean UsuarioDetailsService details;
    @MockitoBean PedidoRepository pedidos;
    @MockitoBean ClienteRepository clientes;
    @MockitoBean CustoDiarioRepository custos;
    @BeforeEach void setup() {
        when(details.loadUserByUsername(anyString())).thenAnswer(inv ->
            (org.springframework.security.core.userdetails.UserDetails) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
    @Test void atendenteNaoAcessaDashboardNemApi() throws Exception {
        for(String rota:List.of("/dashboard","/api/dashboard","/api/dashboard/opcoes"))
            mvc.perform(get(rota).with(user("a").roles("ATENDENTE"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/dashboard/custos/2026-11-05").with(user("a").roles("ATENDENTE")).with(csrf())
            .contentType("application/json").content("{\"valor\":10}")).andExpect(status().isForbidden());
        verifyNoInteractions(pedidos,clientes,custos);
    }
    @Test void apiExigeAutenticacao() throws Exception { mvc.perform(get("/api/dashboard")).andExpect(status().is3xxRedirection()); }
    @Test void adminEGerenteAcessamTelaEApiComDadosReaisDoServico() throws Exception {
        for(String role:List.of("ADMIN","GERENTE")) {
            mvc.perform(get("/dashboard").with(user("u").roles(role))).andExpect(status().isOk())
                .andExpect(content().string(containsString("/dist/css/adminlte.min.css")))
                .andExpect(content().string(containsString("/js/dashboard.js")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
            mvc.perform(get("/api/dashboard?inicio=2026-11-05&fim=2026-11-05").with(user("u").roles(role))).andExpect(status().isOk())
                .andExpect(jsonPath("$.indicadores.faturamento").value(0))
                .andExpect(jsonPath("$.fuso").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.vendasHorario.length()").value(24));
        }
    }
    @Test void apiAgregaPedidoPersistidoSemRetornarEnderecoOuTelefone() throws Exception {
        Pedido p=new Pedido();p.setId("p1");p.setClienteId("c1");p.setClienteNome("Cliente");p.setRegiaoVenda("Mogi / Centro");
        p.setEntrada(Instant.parse("2026-11-05T15:00:00Z"));p.setStatus("FINALIZADO");p.setTotal(new BigDecimal("90.00"));
        when(pedidos.findByEntradaGreaterThanEqualAndEntradaLessThan(any(),any())).thenReturn(List.of(p));
        mvc.perform(get("/api/dashboard?inicio=2026-11-05&fim=2026-11-05").with(user("g").roles("GERENTE"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.indicadores.faturamento").value(90))
            .andExpect(jsonPath("$.clientes[0].clienteId").value("c1"))
            .andExpect(jsonPath("$.clientes[0].telefone").doesNotExist())
            .andExpect(jsonPath("$.clientes[0].logradouro").doesNotExist());
    }
    @Test void geraEvidenciaDeContratoParaPreviaVisual() throws Exception {
        Pedido p=new Pedido();p.setId("p1");p.setClienteId("c1");p.setClienteNome("Cliente de teste");p.setRegiaoVenda("Mogi / Centro");
        p.setEntrada(Instant.parse("2026-11-05T15:00:00Z"));p.setStatus("FINALIZADO");p.setTotal(new BigDecimal("90.00"));
        p.setItens(List.of(new ItemPedido("pizza","Pizza",2,new BigDecimal("50.00"))));
        when(pedidos.findByEntradaGreaterThanEqualAndEntradaLessThan(any(),any())).thenReturn(List.of(p));
        var resposta=mvc.perform(get("/api/dashboard?inicio=2026-11-05&fim=2026-11-05").with(user("g").roles("GERENTE")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.indicadores.ticketMedio").value(90)).andReturn();
        var pagina=mvc.perform(get("/dashboard").with(user("g").roles("GERENTE"))).andExpect(status().isOk()).andReturn();
        var pasta=java.nio.file.Path.of("target","dashboard-preview");java.nio.file.Files.createDirectories(pasta);
        java.nio.file.Files.writeString(pasta.resolve("dados.json"),resposta.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Files.writeString(pasta.resolve("index.html"),pagina.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    @Test void parametrosInvalidosRetornam400() throws Exception {
        for(String query:List.of("inicio=nao-e-data","horaInicio=25:00","inicio=2026-11-06&fim=2026-11-05","agrupamento=ANO","status=INVENTADO")) {
            mvc.perform(get("/api/dashboard?"+query).with(user("g").roles("GERENTE"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        }
    }
    @Test void custoExigeCsrf() throws Exception {
        mvc.perform(put("/api/dashboard/custos/2026-11-05").with(user("g").roles("GERENTE"))
            .contentType("application/json").content("{\"valor\":10}")).andExpect(status().isForbidden());
        verifyNoInteractions(custos);
    }
    @Test void custoValidoPersisteNaColecao() throws Exception {
        when(custos.save(any())).thenAnswer(inv -> inv.getArgument(0));
        mvc.perform(put("/api/dashboard/custos/2026-11-05").with(user("g").roles("GERENTE")).with(csrf())
            .contentType("application/json").content("{\"valor\":120.50,\"observacao\":\"Ingredientes\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.valor").value(120.5)).andExpect(jsonPath("$.atualizadoPor").value("g"));
        verify(custos).save(any(CustoDiario.class));
    }
    @Test void custoInvalidoRetorna400() throws Exception {
        for(String body:List.of("{\"valor\":-1}","{\"valor\":null}","{\"valor\":0.001}","{\"valor\":\"abc\"}")) {
            mvc.perform(put("/api/dashboard/custos/2026-11-05").with(user("g").roles("GERENTE")).with(csrf())
                .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
    }
    @Test void indisponibilidadeDoBancoRetorna503SemDetalhesInternos() throws Exception {
        when(pedidos.findByEntradaGreaterThanEqualAndEntradaLessThan(any(),any())).thenThrow(new DataAccessResourceFailureException("segredo-interno"));
        mvc.perform(get("/api/dashboard").with(user("g").roles("GERENTE"))).andExpect(status().isServiceUnavailable())
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("segredo-interno"))));
    }
}
