package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.CriarPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService service;
    public PedidoController(PedidoService service) { this.service = service; }
    @PostMapping
    public Pedido criarPedido(@RequestBody CriarPedido pedido) { return service.criarPedido(pedido); }
    @GetMapping
    public List<Pedido> listarPedidos() { return service.listarPedidos(); }
    @PutMapping("/{id}/status")
    public Pedido atualizarStatus(@PathVariable String id, @RequestParam String status) { return service.atualizarStatus(id, status); }
    public record Desconto(BigDecimal percentual) { }
    public record Cancelamento(String justificativa) { }
    @PostMapping("/{id}/desconto")
    public Pedido desconto(@PathVariable String id, @RequestBody Desconto entrada) { return service.aplicarDesconto(id, entrada.percentual()); }
    @PostMapping("/{id}/cancelamento")
    public Pedido cancelar(@PathVariable String id, @RequestBody Cancelamento entrada) { return service.cancelar(id, entrada.justificativa()); }
}
