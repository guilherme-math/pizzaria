package com.umc.pediupatrao.dashboard;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record DashboardResumo(DashboardFiltro.Aplicado filtros, Instant atualizadoEm, String fuso,
        Indicadores indicadores, List<Ponto> vendasPeriodo, List<Ponto> vendasHorario,
        List<Ponto> vendasRegiao, List<Situacao> situacaoPedidos, List<ProdutoRanking> produtos,
        List<ClienteRanking> clientes, Custos custos, List<String> regioesDisponiveis, List<String> avisos) {
    public record Indicadores(BigDecimal faturamento, long pedidosFinalizados, BigDecimal ticketMedio,
            long totalPedidos, long cancelados, BigDecimal descontos, long clientesAtendidos) { }
    public record Ponto(String rotulo, long pedidos, BigDecimal faturamento) { }
    public record Situacao(String status, long quantidade) { }
    public record ProdutoRanking(String produtoId, String nome, long quantidade, BigDecimal valorBruto) { }
    public record ClienteRanking(String clienteId, String nome, long pedidos, BigDecimal faturamento, BigDecimal ticketMedio) { }
    public record Custos(BigDecimal totalInformado, long diasInformados, long diasPeriodo, boolean completo,
            boolean resultadoDisponivel, BigDecimal resultadoOperacional, List<DiaCusto> dias) { }
    public record DiaCusto(String data, BigDecimal valor, String observacao, String atualizadoPor, Instant atualizadoEm) { }
    public record ClienteOpcao(String id, String nome) { }
    public record Opcoes(List<ClienteOpcao> clientes, List<String> status) { }
}
