package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service
public class ClienteService {
    private final ClienteRepository clientes;
    private final PedidoRepository pedidos;
    private final AuditoriaService auditoria;
    public ClienteService(ClienteRepository clientes, PedidoRepository pedidos, AuditoriaService auditoria) {
        this.clientes = clientes; this.pedidos = pedidos; this.auditoria = auditoria;
    }
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public List<Cliente> listarClientes() { return clientes.findAll(); }
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Optional<Cliente> buscarPorId(String id) { return clientes.findById(id); }

    @Transactional
    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente novoCliente(Cliente cliente) { return salvar(cliente); }

    @Transactional
    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente salvar(Cliente dados) {
        if (dados.getNome() == null || dados.getNome().isBlank() || dados.getTelefone() == null || dados.getTelefone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe nome e telefone");
        }
        boolean novo = dados.getId() == null || dados.getId().isBlank();
        Cliente cliente = novo ? new Cliente() : clientes.findById(dados.getId()).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente nao encontrado"));
        var anterior = auditoria.snapshot(novo ? null : cliente);
        cliente.setNome(dados.getNome().trim()); cliente.setTelefone(dados.getTelefone().trim());
        cliente.setCep(dados.getCep()); cliente.setLogradouro(dados.getLogradouro()); cliente.setNumero(dados.getNumero());
        cliente.setComplemento(dados.getComplemento()); cliente.setBairro(dados.getBairro());
        cliente.setCidade(dados.getCidade()); cliente.setEstado(dados.getEstado()); cliente.setEndereco(dados.getEndereco());
        clientes.save(cliente);
        auditoria.registrar(novo ? "INCLUSAO_CLIENTE" : "ALTERACAO_CLIENTE", "Cliente", cliente.getId(), anterior, cliente, null);
        return cliente;
    }
    @Transactional
    @PreAuthorize("hasRole('ATENDENTE')")
    public void excluir(String id) {
        Cliente cliente = clientes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente nao encontrado"));
        if (pedidos.existsByClienteId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cliente possui pedidos; preserve seu historico");
        }
        var anterior = auditoria.snapshot(cliente);
        clientes.delete(cliente);
        auditoria.registrar("EXCLUSAO_CLIENTE", "Cliente", id, anterior, null, null);
    }
}
