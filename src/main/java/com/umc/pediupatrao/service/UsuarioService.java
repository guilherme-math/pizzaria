package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Optional;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Usuario> buscarPorId(String id) { return usuarioRepository.findById(id); }
    public Optional<Usuario> buscarPorUsername(String username) { return usuarioRepository.findByUsername(username); }
    public Iterable<Usuario> listarTodos() { return usuarioRepository.findAll(); }

    public void salvarUsuario(Usuario usuario) {
        validar(usuario, null);
        if (usuario.getId() != null && !usuario.getId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Novo usuario nao pode informar ID");
        }
        if (usuario.getPassword() == null || usuario.getPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter pelo menos 8 caracteres");
        }
        usuario.setId(null);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuarioRepository.save(usuario);
    }

    public void atualizarUsuario(String id, Usuario dados) {
        Usuario atual = usuarioRepository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
        validar(dados, id);
        String autenticado = SecurityContextHolder.getContext().getAuthentication().getName();
        if (atual.getUsername().equals(autenticado) &&
                (!atual.getRole().equals(dados.getRole()) || !atual.getUsername().equals(dados.getUsername()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao altere seu proprio perfil ou login");
        }
        atual.setUsername(dados.getUsername());
        atual.setRole(dados.getRole());
        if (dados.getPassword() != null && !dados.getPassword().isBlank()) {
            if (dados.getPassword().length() < 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter pelo menos 8 caracteres");
            }
            atual.setPassword(passwordEncoder.encode(dados.getPassword()));
        }
        usuarioRepository.save(atual);
    }

    public void deletarUsuario(String id) {
        Usuario atual = usuarioRepository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
        if (atual.getUsername().equals(SecurityContextHolder.getContext().getAuthentication().getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voce nao pode excluir sua propria conta");
        }
        usuarioRepository.deleteById(id);
    }

    private void validar(Usuario usuario, String id) {
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o login");
        }
        usuario.setUsername(usuario.getUsername().trim());
        try { Perfil.valueOf(usuario.getRole()); }
        catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Perfil invalido");
        }
        usuarioRepository.findByUsername(usuario.getUsername()).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Login ja cadastrado");
            }
        });
    }
}
