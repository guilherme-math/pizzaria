package com.umc.pediupatrao;

import com.umc.pediupatrao.dashboard.*;
import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock PedidoRepository pedidos;
    @Mock ClienteRepository clientes;
    @Mock CustoDiarioRepository custos;
    DashboardService service;
    Clock clock = Clock.fixed(Instant.parse("2026-11-05T15:00:00Z"), ZoneOffset.UTC);
    @BeforeEach void setup() {
        service = new DashboardService(pedidos,clientes,custos,clock);
        lenient().when(clientes.findAll()).thenReturn(List.of());
        lenient().when(custos.findByIdGreaterThanEqualAndIdLessThanEqualOrderById(anyString(),anyString())).thenReturn(List.of());
        lenient().when(pedidos.findByEntradaGreaterThanEqualAndEntradaLessThan(any(),any())).thenReturn(List.of());
    }
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }
    DashboardFiltro filtro() { var f = new DashboardFiltro(); f.setInicio(LocalDate.of(2026,11,5));f.setFim(f.getInicio());return f; }
    Pedido pedido(String id, String status, String entrada, String total) {
        Pedido p = new Pedido(); p.setId(id);p.setClienteId("c1");p.setClienteNome("Bruno");p.setRegiaoVenda("Mogi / Centro");
        p.setStatus(status);p.setEntrada(Instant.parse(entrada));p.setTotal(new BigDecimal(total));
        p.setItens(List.of(new ItemPedido("pizza","Pizza",2,new BigDecimal("50.00"))));return p;
    }
    void dados(Pedido... valores) { when(pedidos.findByEntradaGreaterThanEqualAndEntradaLessThan(any(),any())).thenReturn(List.of(valores)); }
    @Test void financeiroSomenteFinalizadosEValoresLiquidos() {
        Pedido venda = pedido("1","FINALIZADO","2026-11-05T15:00:00Z","90.00");venda.setValorDesconto(new BigDecimal("10.00"));
        dados(venda,pedido("2","CANCELADO","2026-11-05T16:00:00Z","100.00"),pedido("3","RECEBIDO","2026-11-05T16:00:00Z","100.00"));
        var r=service.consultar(filtro());assertEquals(new BigDecimal("90.00"),r.indicadores().faturamento());
        assertEquals(3,r.indicadores().totalPedidos());assertEquals(1,r.indicadores().cancelados());assertEquals(new BigDecimal("90.00"),r.indicadores().ticketMedio());
        assertEquals(new BigDecimal("10.00"),r.indicadores().descontos());
        assertEquals(2,r.produtos().get(0).quantidade());assertEquals(new BigDecimal("100.00"),r.produtos().get(0).valorBruto());
    }
    @Test void vazioRetornaZeroSemDivisaoPorZeroEPreencheHorasEDias() {
        var r=service.consultar(filtro());assertEquals(new BigDecimal("0.00"),r.indicadores().ticketMedio());
        assertEquals(24,r.vendasHorario().size());assertEquals(1,r.vendasPeriodo().size());assertTrue(r.produtos().isEmpty());
        assertNull(r.custos().resultadoOperacional());assertFalse(r.custos().completo());
    }
    @Test void consultaMongoUsaLimitesDoFusoLocal() {
        service.consultar(filtro());
        verify(pedidos).findByEntradaGreaterThanEqualAndEntradaLessThan(Instant.parse("2026-11-05T03:00:00Z"),Instant.parse("2026-11-06T03:00:00Z"));
    }
    @Test void meiaNoiteUsaDiaLocalNaoDiaUtc() {
        dados(pedido("1","FINALIZADO","2026-11-06T01:30:00Z","50"));
        var r=service.consultar(filtro());assertEquals(1,r.vendasHorario().get(22).pedidos());assertEquals("2026-11-05",r.vendasPeriodo().get(0).rotulo());
    }
    @Test void faixaDeHorarioNoturnaAceitaAntesEDepoisDaMeiaNoite() {
        var f=filtro();f.setHoraInicio(LocalTime.of(22,0));f.setHoraFim(LocalTime.of(2,0));
        dados(pedido("1","FINALIZADO","2026-11-05T04:00:00Z","30"),pedido("2","FINALIZADO","2026-11-06T02:00:00Z","50"),pedido("3","FINALIZADO","2026-11-05T18:00:00Z","100"));
        assertEquals(new BigDecimal("80.00"),service.consultar(f).indicadores().faturamento());
    }
    @Test void limiteFinalDaFaixaEhExclusivo() {
        var f=filtro();f.setHoraInicio(LocalTime.of(10,0));f.setHoraFim(LocalTime.of(11,0));
        dados(pedido("1","FINALIZADO","2026-11-05T13:00:00Z","30"),pedido("2","FINALIZADO","2026-11-05T14:00:00Z","50"));
        assertEquals(new BigDecimal("30.00"),service.consultar(f).indicadores().faturamento());
    }
    @Test void filtraRegiaoClienteEStatus() {
        var f=filtro();f.setRegiao("Mogi / Centro");f.setClienteId("c1");f.setStatus("FINALIZADO");
        var outro=pedido("2","FINALIZADO","2026-11-05T15:00:00Z","50");outro.setClienteId("c2");
        dados(pedido("1","FINALIZADO","2026-11-05T15:00:00Z","90"),outro,pedido("3","RECEBIDO","2026-11-05T15:00:00Z","100"));
        assertEquals(1,service.consultar(f).indicadores().totalPedidos());
    }
    @Test void regiaoHistoricaNaoMudaComEnderecoAtual() {
        Cliente c=new Cliente();c.setId("c1");c.setCidade("Outra cidade");c.setBairro("Outro bairro");when(clientes.findAll()).thenReturn(List.of(c));
        dados(pedido("1","FINALIZADO","2026-11-05T15:00:00Z","50"));
        assertEquals("Mogi / Centro",service.consultar(filtro()).vendasRegiao().get(0).rotulo());
    }
    @Test void legadoUsaCadastroAtualComAviso() {
        var p=pedido("1","FINALIZADO","2026-11-05T15:00:00Z","50");p.setRegiaoVenda(null);
        Cliente c=new Cliente();c.setId("c1");c.setCidade("Mogi");c.setBairro("Vila");when(clientes.findAll()).thenReturn(List.of(c));dados(p);
        var r=service.consultar(filtro());assertEquals("Mogi / Vila",r.vendasRegiao().get(0).rotulo());assertTrue(r.avisos().stream().anyMatch(a->a.contains("cadastro atual")));
    }
    @Test void agrupamentoMensalConsolidaDias() {
        var f=filtro();f.setFim(LocalDate.of(2026,11,6));f.setAgrupamento("MES");
        dados(pedido("1","FINALIZADO","2026-11-05T15:00:00Z","50"),pedido("2","FINALIZADO","2026-11-06T15:00:00Z","70"));
        var r=service.consultar(f);assertEquals(1,r.vendasPeriodo().size());assertEquals(new BigDecimal("120.00"),r.vendasPeriodo().get(0).faturamento());
    }
    @Test void dadosIncompletosNaoQuebramPainel() {
        var p=pedido("1","FINALIZADO","2026-11-05T15:00:00Z","50");p.setTotal(null);p.setItens(null);dados(p);
        when(pedidos.countByEntradaIsNull()).thenReturn(2L);
        var r=service.consultar(filtro());assertEquals(new BigDecimal("0.00"),r.indicadores().faturamento());assertTrue(r.avisos().size()>=2);
    }
    @Test void custosCompletosPermitemResultadoOperacional() {
        var c=new CustoDiario();c.setId("2026-11-05");c.setValor(new BigDecimal("30.00"));
        when(custos.findByIdGreaterThanEqualAndIdLessThanEqualOrderById(anyString(),anyString())).thenReturn(List.of(c));
        dados(pedido("1","FINALIZADO","2026-11-05T15:00:00Z","90"));
        var r=service.consultar(filtro());assertTrue(r.custos().completo());assertEquals(new BigDecimal("60.00"),r.custos().resultadoOperacional());
        var f=filtro();f.setClienteId("c1");assertNull(service.consultar(f).custos().resultadoOperacional());
    }
    @Test void rejeitaPeriodosInvalidosAntesDeConsultarBanco() {
        var f=filtro();f.setFim(f.getInicio().minusDays(1));assertThrows(ResponseStatusException.class,()->service.consultar(f));
        f.setFim(f.getInicio().plusDays(366));assertThrows(ResponseStatusException.class,()->service.consultar(f));
        verifyNoInteractions(pedidos);
    }
    @Test void rejeitaHorarioParcialEStatusInvalido() {
        var f=filtro();f.setHoraInicio(LocalTime.NOON);assertThrows(ResponseStatusException.class,()->service.consultar(f));
        f.setHoraFim(LocalTime.NOON);assertThrows(ResponseStatusException.class,()->service.consultar(f));
        f.setHoraInicio(null);f.setHoraFim(null);f.setStatus("INVENTADO");assertThrows(ResponseStatusException.class,()->service.consultar(f));
    }
    @Test void rejeitaCustoNegativoEPrecisaoInvalida() {
        assertThrows(ResponseStatusException.class,()->service.salvarCusto(LocalDate.of(2026,11,5),new BigDecimal("-1"),""));
        assertThrows(ResponseStatusException.class,()->service.salvarCusto(LocalDate.of(2026,11,5),new BigDecimal("1.001"),""));
        verifyNoInteractions(custos);
    }
    @Test void salvaCustoComIdentidadeESemInventarValor() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("gerente","",List.of()));
        when(custos.findById("2026-11-05")).thenReturn(Optional.empty());when(custos.save(any())).thenAnswer(inv->inv.getArgument(0));
        var c=service.salvarCusto(LocalDate.of(2026,11,5),BigDecimal.ZERO,"Sem despesas");
        assertEquals(new BigDecimal("0.00"),c.getValor());assertEquals("gerente",c.getAtualizadoPor());assertEquals(clock.instant(),c.getAtualizadoEm());
    }
}
