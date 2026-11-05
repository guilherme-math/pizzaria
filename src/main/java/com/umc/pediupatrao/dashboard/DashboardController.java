package com.umc.pediupatrao.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }
    @GetMapping("/dashboard") public String tela() { return "dashboard"; }
    @GetMapping("/api/dashboard") @ResponseBody
    public DashboardResumo consultar(@ModelAttribute DashboardFiltro filtro) { return service.consultar(filtro); }
    @GetMapping("/api/dashboard/opcoes") @ResponseBody
    public DashboardResumo.Opcoes opcoes() { return service.opcoes(); }
    public record CustoEntrada(BigDecimal valor, String observacao) { }
    @PutMapping("/api/dashboard/custos/{data}") @ResponseBody
    public CustoDiario salvar(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestBody CustoEntrada entrada) { return service.salvarCusto(data, entrada.valor(), entrada.observacao()); }
}
