package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import static com.umc.pediupatrao.dashboard.DashboardResumo.*;

@Service
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class DashboardService {
    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final CustoDiarioRepository custos;
    private final Clock clock;
    public DashboardService(PedidoRepository pedidos, ClienteRepository clientes, CustoDiarioRepository custos, Clock clock) {
        this.pedidos = pedidos; this.clientes = clientes; this.custos = custos; this.clock = clock;
    }
    public DashboardResumo consultar(DashboardFiltro filtro) {
        var f = filtro.aplicar(clock);
        Instant inicio = f.inicio().atStartOfDay(DashboardFiltro.FUSO).toInstant();
        Instant fim = f.fim().plusDays(1).atStartOfDay(DashboardFiltro.FUSO).toInstant();
        List<Pedido> periodo = pedidos.findByEntradaGreaterThanEqualAndEntradaLessThan(inicio, fim);
        Map<String, Cliente> cadastro = new HashMap<>();
        clientes.findAll().forEach(c -> cadastro.put(c.getId(), c));
        Function<Pedido,String> local = p -> p.getRegiaoVenda() == null ? regiao(cadastro.get(p.getClienteId())) : p.getRegiaoVenda();
        List<String> regioes = periodo.stream().map(local).distinct().sorted().toList();
        List<Pedido> selecionados = periodo.stream().filter(p -> p.getEntrada() != null)
            .filter(p -> f.aceitaHora(p.getEntrada().atZone(DashboardFiltro.FUSO).toLocalTime()))
            .filter(p -> f.regiao() == null || f.regiao().equals(local.apply(p)))
            .filter(p -> f.clienteId() == null || f.clienteId().equals(p.getClienteId()))
            .filter(p -> f.status() == null || f.status().equals(p.getStatus())).toList();
        List<Pedido> vendas = selecionados.stream().filter(p -> "FINALIZADO".equals(p.getStatus()))
            .filter(p -> p.getTotal() != null && p.getTotal().signum() >= 0).toList();
        BigDecimal faturamento = soma(vendas, Pedido::getTotal);
        long cancelados = selecionados.stream().filter(p -> "CANCELADO".equals(p.getStatus())).count();
        long clientesAtendidos = vendas.stream().map(Pedido::getClienteId).filter(Objects::nonNull).distinct().count();
        var indicadores = new Indicadores(faturamento, vendas.size(), media(faturamento, vendas.size()), selecionados.size(),
            cancelados, soma(vendas, Pedido::getValorDesconto), clientesAtendidos);
        Map<String, List<Pedido>> porPeriodo = new TreeMap<>();
        for (LocalDate d = f.inicio(); !d.isAfter(f.fim()); d = d.plusDays(1)) {
            porPeriodo.putIfAbsent("MES".equals(f.agrupamento()) ? YearMonth.from(d).toString() : d.toString(), new ArrayList<>());
        }
        for (Pedido p : vendas) {
            LocalDate d = p.getEntrada().atZone(DashboardFiltro.FUSO).toLocalDate();
            porPeriodo.get("MES".equals(f.agrupamento()) ? YearMonth.from(d).toString() : d.toString()).add(p);
        }
        List<Ponto> serie = porPeriodo.entrySet().stream().map(e -> ponto(e.getKey(), e.getValue())).toList();
        List<Ponto> horas = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            int hora = h;
            horas.add(ponto(String.format("%02d:00", h), vendas.stream()
                .filter(p -> p.getEntrada().atZone(DashboardFiltro.FUSO).getHour() == hora).toList()));
        }
        Map<String,List<Pedido>> porRegiao = new HashMap<>();
        vendas.forEach(p -> porRegiao.computeIfAbsent(local.apply(p), k -> new ArrayList<>()).add(p));
        List<Ponto> regioesVenda = porRegiao.entrySet().stream().map(e -> ponto(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(Ponto::faturamento).reversed().thenComparing(Ponto::rotulo)).toList();
        List<Situacao> estados = new ArrayList<>();
        for (StatusPedido status : StatusPedido.values()) estados.add(new Situacao(status.name(),
            selecionados.stream().filter(p -> status.name().equals(p.getStatus())).count()));
        long outros = selecionados.stream().filter(p -> Arrays.stream(StatusPedido.values()).noneMatch(s -> s.name().equals(p.getStatus()))).count();
        if (outros > 0) estados.add(new Situacao("LEGADO", outros));
        Map<String,ProdutoRanking> ranking = new HashMap<>();
        long itensInvalidos = 0;
        for (Pedido p : vendas) {
            if (p.getItens() == null || p.getItens().isEmpty()) { itensInvalidos++; continue; }
            for (ItemPedido item : p.getItens()) {
                if (item == null || item.produtoId() == null || item.quantidade() <= 0 || item.precoUnitario() == null || item.precoUnitario().signum() < 0) {
                    itensInvalidos++; continue;
                }
                ProdutoRanking anterior = ranking.get(item.produtoId());
                ranking.put(item.produtoId(), new ProdutoRanking(item.produtoId(), item.nome(), item.quantidade() + (anterior == null ? 0 : anterior.quantidade()),
                    dinheiro(item.getSubtotal().add(anterior == null ? BigDecimal.ZERO : anterior.valorBruto()))));
            }
        }
        List<ProdutoRanking> top = ranking.values().stream().sorted(Comparator.comparingLong(ProdutoRanking::quantidade).reversed()
            .thenComparing(ProdutoRanking::produtoId)).limit(10).toList();
        Map<String,List<Pedido>> porCliente = new HashMap<>();
        vendas.forEach(p -> porCliente.computeIfAbsent(p.getClienteId() == null ? "SEM_CLIENTE" : p.getClienteId(), k -> new ArrayList<>()).add(p));
        List<ClienteRanking> topClientes = porCliente.entrySet().stream().map(e -> {
            var lista = e.getValue(); BigDecimal valor = soma(lista, Pedido::getTotal);
            String nome = lista.get(0).getClienteNome();
            if (nome == null || nome.isBlank()) nome = cadastro.containsKey(e.getKey()) ? cadastro.get(e.getKey()).getNome() : "Cliente nao identificado";
            return new ClienteRanking(e.getKey(), nome, lista.size(), valor, media(valor, lista.size()));
        }).sorted(Comparator.comparing(ClienteRanking::faturamento).reversed().thenComparing(ClienteRanking::clienteId)).limit(10).toList();
        List<CustoDiario> registros = custos.findByIdGreaterThanEqualAndIdLessThanEqualOrderById(f.inicio().toString(), f.fim().toString());
        BigDecimal totalCustos = dinheiro(registros.stream().map(CustoDiario::getValor).reduce(BigDecimal.ZERO, BigDecimal::add));
        long dias = ChronoUnit.DAYS.between(f.inicio(), f.fim()) + 1;
        boolean completo = registros.size() == dias;
        boolean resultado = completo && !f.segmentado();
        var dadosCustos = new Custos(totalCustos, registros.size(), dias, completo, resultado,
            resultado ? dinheiro(faturamento.subtract(totalCustos)) : null,
            registros.stream().map(c -> new DiaCusto(c.getId(), c.getValor(), c.getObservacao(), c.getAtualizadoPor(), c.getAtualizadoEm())).toList());
        List<String> avisos = new ArrayList<>();
        long semData = pedidos.countByEntradaIsNull();
        if (semData > 0) avisos.add(semData + " pedidos sem data de entrada nao participam das consultas por periodo.");
        long regiaoAtual = selecionados.stream().filter(p -> p.getRegiaoVenda() == null).count();
        if (regiaoAtual > 0) avisos.add("Para " + regiaoAtual + " pedidos antigos, a regiao usa o cadastro atual do cliente.");
        long totalInvalido = selecionados.stream().filter(p -> "FINALIZADO".equals(p.getStatus()) && (p.getTotal() == null || p.getTotal().signum() < 0)).count();
        if (totalInvalido > 0) avisos.add(totalInvalido + " pedidos finalizados com total invalido foram excluidos dos indicadores financeiros.");
        if (itensInvalidos > 0) avisos.add(itensInvalidos + " pedidos ou itens sem detalhamento valido nao participam do ranking de produtos.");
        if (!completo) avisos.add("Custos incompletos: informe todos os dias do periodo para calcular o resultado operacional.");
        if (f.segmentado()) avisos.add("Custos sao gerais do periodo, sem rateio por cliente, regiao, horario ou status. O resultado operacional fica indisponivel nesse recorte.");
        return new DashboardResumo(f, clock.instant(), DashboardFiltro.FUSO.getId(), indicadores, serie, horas, regioesVenda, estados,
            top, topClientes, dadosCustos, regioes, avisos);
    }
    public Opcoes opcoes() {
        return new Opcoes(clientes.findAll().stream().sorted(Comparator.comparing(c -> Objects.toString(c.getNome(), "")))
            .map(c -> new ClienteOpcao(c.getId(), c.getNome())).toList(), Arrays.stream(StatusPedido.values()).map(Enum::name).toList());
    }
    public CustoDiario salvarCusto(LocalDate data, BigDecimal valor, String observacao) {
        if (data == null || data.getYear() < 2000 || data.getYear() > 2100 || valor == null || valor.signum() < 0
                || valor.compareTo(new BigDecimal("999999999.99")) > 0 || valor.stripTrailingZeros().scale() > 2
                || observacao != null && observacao.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe data valida, custo nao negativo com ate duas casas decimais e observacao de ate 500 caracteres");
        }
        CustoDiario custo = custos.findById(data.toString()).orElseGet(CustoDiario::new);
        custo.setId(data.toString()); custo.setValor(dinheiro(valor)); custo.setObservacao(observacao == null ? "" : observacao.trim());
        custo.setAtualizadoEm(clock.instant()); custo.setAtualizadoPor(SecurityContextHolder.getContext().getAuthentication().getName());
        return custos.save(custo);
    }
    public static String regiao(Cliente cliente) {
        if (cliente == null) return "Sem regiao";
        String cidade = cliente.getCidade() == null ? "" : cliente.getCidade().trim();
        String bairro = cliente.getBairro() == null ? "" : cliente.getBairro().trim();
        String valor = cidade.isEmpty() ? bairro : bairro.isEmpty() ? cidade : cidade + " / " + bairro;
        return valor.isEmpty() ? "Sem regiao" : valor;
    }
    private static Ponto ponto(String nome, List<Pedido> vendas) { return new Ponto(nome, vendas.size(), soma(vendas, Pedido::getTotal)); }
    private static BigDecimal soma(List<Pedido> valores, Function<Pedido,BigDecimal> campo) {
        return dinheiro(valores.stream().map(campo).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
    private static BigDecimal dinheiro(BigDecimal valor) { return valor.setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal media(BigDecimal total, long quantidade) { return quantidade == 0 ? dinheiro(BigDecimal.ZERO) : total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP); }
}
