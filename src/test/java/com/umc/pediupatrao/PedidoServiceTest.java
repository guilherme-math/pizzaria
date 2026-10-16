package com.umc.pediupatrao;

import com.umc.pediupatrao.dto.CriarPedido;
import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.*;
import com.umc.pediupatrao.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {
    @Mock PedidoRepository pedidos;
    @Mock ClienteRepository clientes;
    @Mock ProdutoRepository produtos;
    @Mock AuditoriaService auditoria;
    PedidoService service;
    @BeforeEach void setup() {
        service = new PedidoService(pedidos, clientes, produtos, auditoria);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("operador", "", List.of()));
    }
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }
    Pedido pedido(String status) {
        Pedido p = new Pedido(); p.setId("p1"); p.setStatus(status); p.setSubtotal(new BigDecimal("100.00")); p.setTotal(p.getSubtotal());
        when(pedidos.findById("p1")).thenReturn(Optional.of(p)); return p;
    }
    @Test void calculaComPrecoDoBancoERegistraEntrada() {
        Cliente c = new Cliente(); c.setId("c1"); c.setNome("Cliente");
        Produto produto = new Produto(); produto.setId("prod1"); produto.setNome("Pizza"); produto.setPreco(39.90); produto.setAtivo(true);
        when(clientes.findById("c1")).thenReturn(Optional.of(c)); when(produtos.findById("prod1")).thenReturn(Optional.of(produto));
        Pedido p = service.criarPedido(new CriarPedido("c1", List.of(new CriarPedido.Item("prod1", 2))));
        assertEquals(new BigDecimal("79.80"), p.getTotal()); assertEquals("RECEBIDO", p.getStatus());
        assertEquals("operador", p.getResponsavelEntrada()); assertNotNull(p.getEntrada());
        verify(auditoria).registrar(eq("INCLUSAO_PEDIDO"), eq("Pedido"), any(), eq(Map.of()), eq(p), isNull());
    }
    @Test void rejeitaQuantidadeZero() {
        when(clientes.findById("c1")).thenReturn(Optional.of(new Cliente()));
        assertThrows(ResponseStatusException.class, () -> service.criarPedido(new CriarPedido("c1", List.of(new CriarPedido.Item("x", 0)))));
        verify(pedidos, never()).save(any());
    }
    @Test void naoPermitePularEtapas() {
        pedido("EM_PREPARACAO"); assertThrows(ResponseStatusException.class, () -> service.atualizarStatus("p1", "FINALIZADO"));
        verify(pedidos, never()).save(any());
    }
    @Test void registraSaidaEResponsavel() {
        Pedido p = pedido("PRONTO"); service.atualizarStatus("p1", "RETIRADO");
        assertNotNull(p.getSaida()); assertEquals("operador", p.getResponsavelSaida());
        verify(auditoria).registrar(eq("MUDANCA_STATUS"), eq("Pedido"), eq("p1"), any(), eq(p), isNull());
    }
    @Test void descontoSubstituiAnteriorSemAcumular() {
        Pedido p = pedido("RECEBIDO"); service.aplicarDesconto("p1", new BigDecimal("10")); service.aplicarDesconto("p1", new BigDecimal("20"));
        assertEquals(new BigDecimal("80.00"), p.getTotal()); assertEquals(new BigDecimal("20.00"), p.getValorDesconto());
    }
    @Test void rejeitaDescontoAcimaDoLimiteENegativo() {
        pedido("RECEBIDO");
        for (String percentual : List.of("20.01", "-1")) assertThrows(ResponseStatusException.class, () -> service.aplicarDesconto("p1", new BigDecimal(percentual)));
    }
    @Test void bloqueiaDescontoECancelamentoAposSaida() {
        pedido("SAIU_PARA_ENTREGA");
        assertThrows(ResponseStatusException.class, () -> service.aplicarDesconto("p1", BigDecimal.TEN));
        assertThrows(ResponseStatusException.class, () -> service.cancelar("p1", "Cliente desistiu"));
    }
    @Test void exigeJustificativa() {
        pedido("RECEBIDO"); assertThrows(ResponseStatusException.class, () -> service.cancelar("p1", "   "));
    }
    @Test void cancelamentoPreservaDadosEBloqueiaAlteracoes() {
        Pedido p = pedido("RECEBIDO"); service.cancelar("p1", "Cliente desistiu");
        assertEquals("CANCELADO", p.getStatus()); assertEquals(new BigDecimal("100.00"), p.getTotal());
        assertThrows(ResponseStatusException.class, () -> service.atualizarStatus("p1", "EM_PREPARACAO"));
        assertThrows(ResponseStatusException.class, () -> service.aplicarDesconto("p1", BigDecimal.TEN));
        assertThrows(ResponseStatusException.class, () -> service.cancelar("p1", "Outra tentativa"));
        verify(auditoria).registrar(eq("CANCELAMENTO"), eq("Pedido"), eq("p1"), any(), eq(p), eq("Cliente desistiu"));
    }
    @Test void fluxoTemSomenteAsTransicoesEsperadas() {
        for (StatusPedido atual : StatusPedido.values()) for (StatusPedido novo : StatusPedido.values()) {
            boolean esperado = switch (atual) {
                case RECEBIDO -> novo == StatusPedido.EM_PREPARACAO;
                case EM_PREPARACAO -> novo == StatusPedido.PRONTO;
                case PRONTO -> novo == StatusPedido.RETIRADO || novo == StatusPedido.SAIU_PARA_ENTREGA;
                case RETIRADO, SAIU_PARA_ENTREGA -> novo == StatusPedido.FINALIZADO;
                default -> false;
            };
            assertEquals(esperado, atual.permite(novo), atual + " -> " + novo);
        }
    }
}
