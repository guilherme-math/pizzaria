package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.CriarPedido;
import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.*;
import java.time.Instant;
import java.util.*;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final ProdutoRepository produtos;
    private final AuditoriaService auditoria;
    public PedidoService(PedidoRepository pedidos, ClienteRepository clientes, ProdutoRepository produtos, AuditoriaService auditoria) {
        this.pedidos = pedidos; this.clientes = clientes; this.produtos = produtos; this.auditoria = auditoria;
    }
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Pedido> listarPedidos() { return pedidos.findAll(); }

    @Transactional
    @PreAuthorize("hasRole('ATENDENTE')")
    public Pedido criarPedido(CriarPedido entrada) {
        exigir(entrada.clienteId() != null && !entrada.clienteId().isBlank(), "Informe o cliente");
        Cliente cliente = clientes.findById(entrada.clienteId()).orElseThrow(() -> erro("Cliente nao encontrado"));
        exigir(entrada.itens() != null && !entrada.itens().isEmpty() && entrada.itens().size() <= 100, "Informe de 1 a 100 itens");
        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId()); pedido.setClienteNome(cliente.getNome());
        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CriarPedido.Item item : entrada.itens()) {
            exigir(item != null && item.produtoId() != null && item.quantidade() > 0 && item.quantidade() <= 999, "Item ou quantidade invalida");
            Produto produto = produtos.findById(item.produtoId()).orElseThrow(() -> erro("Produto nao encontrado"));
            exigir(Boolean.TRUE.equals(produto.getAtivo()), "Produto inativo");
            exigir(produto.getPreco() != null && Double.isFinite(produto.getPreco()) && produto.getPreco() >= 0, "Preco invalido");
            BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
            ItemPedido salvo = new ItemPedido(produto.getId(), produto.getNome(), item.quantidade(), preco);
            itens.add(salvo); subtotal = subtotal.add(salvo.getSubtotal());
        }
        pedido.setItens(itens); pedido.setSubtotal(subtotal); pedido.setTotal(subtotal);
        pedido.setStatus(StatusPedido.RECEBIDO.name());
        pedido.setEntrada(Instant.now()); pedido.setResponsavelEntrada(usuario());
        pedidos.save(pedido);
        auditoria.registrar("INCLUSAO_PEDIDO", "Pedido", pedido.getId(), Map.of(), pedido, null);
        return pedido;
    }
    @Transactional
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = obter(id);
        StatusPedido atual = estado(pedido.getStatus());
        StatusPedido novo = estado(status);
        exigir(atual.permite(novo), "Transicao de " + atual + " para " + novo + " nao permitida");
        var anterior = auditoria.snapshot(pedido);
        pedido.setStatus(novo.name());
        if (novo == StatusPedido.SAIU_PARA_ENTREGA || novo == StatusPedido.RETIRADO) {
            pedido.setSaida(Instant.now()); pedido.setResponsavelSaida(usuario());
        }
        return salvar(pedido, anterior, "MUDANCA_STATUS", null);
    }
    @Transactional
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual) {
        Pedido pedido = obter(id);
        exigir(estado(pedido.getStatus()).antesDaSaida(), "Desconto permitido somente antes da saida e em pedido nao cancelado");
        exigir(percentual != null && percentual.signum() >= 0 && percentual.compareTo(new BigDecimal("20")) <= 0, "Desconto deve estar entre 0 e 20 por cento");
        exigir(percentual.stripTrailingZeros().scale() <= 2, "Informe no maximo duas casas decimais");
        var anterior = auditoria.snapshot(pedido);
        BigDecimal desconto = pedido.getSubtotal().multiply(percentual).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        pedido.setPercentualDesconto(percentual); pedido.setValorDesconto(desconto);
        pedido.setTotal(pedido.getSubtotal().subtract(desconto));
        return salvar(pedido, anterior, "DESCONTO", null);
    }
    @Transactional
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, String justificativa) {
        Pedido pedido = obter(id);
        exigir(estado(pedido.getStatus()).antesDaSaida(), "Cancelamento permitido somente antes da saida");
        exigir(justificativa != null && !justificativa.isBlank() && justificativa.length() <= 1000, "Informe justificativa de ate 1000 caracteres");
        var anterior = auditoria.snapshot(pedido);
        pedido.setJustificativaCancelamento(justificativa.trim()); pedido.setStatus(StatusPedido.CANCELADO.name());
        return salvar(pedido, anterior, "CANCELAMENTO", justificativa.trim());
    }
    private Pedido salvar(Pedido pedido, Map<String,Object> anterior, String operacao, String justificativa) {
        pedidos.save(pedido);
        auditoria.registrar(operacao, "Pedido", pedido.getId(), anterior, pedido, justificativa);
        return pedido;
    }
    private Pedido obter(String id) { return pedidos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado")); }
    private StatusPedido estado(String status) {
        try { return StatusPedido.valueOf(status); }
        catch (IllegalArgumentException | NullPointerException e) { throw erro("Status invalido ou pedido legado; registre novo pedido para o fluxo atual"); }
    }
    private String usuario() { return SecurityContextHolder.getContext().getAuthentication().getName(); }
    private static void exigir(boolean condicao, String mensagem) { if (!condicao) throw erro(mensagem); }
    private static ResponseStatusException erro(String mensagem) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem); }
}
