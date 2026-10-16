package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@Service
public class ProdutoService {
    private final ProdutoRepository produtos;
    public ProdutoService(ProdutoRepository produtos) { this.produtos = produtos; }
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Produto> listarProdutos() { return produtos.findAll(); }
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Produto novoProduto(Produto produto) { return salvar(produto); }
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Produto salvar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank() || produto.getTipo() == null || produto.getTipo().isBlank()
                || produto.getPreco() == null || !Double.isFinite(produto.getPreco()) || produto.getPreco() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe nome, tipo e preco valido");
        }
        if (produto.getId() != null && !produto.getId().isBlank() && !produtos.existsById(produto.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto nao encontrado");
        }
        if (produto.getId() != null && produto.getId().isBlank()) produto.setId(null);
        return produtos.save(produto);
    }
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public void excluir(String id) {
        if (!produtos.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto nao encontrado");
        produtos.deleteById(id);
    }
}
