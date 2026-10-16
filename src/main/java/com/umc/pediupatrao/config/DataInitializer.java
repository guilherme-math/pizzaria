package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            // Migra perfis do projeto original sem redefinir senhas existentes.
            for (Usuario usuario : repo.findAll()) {
                if (usuario.getRole() == null || usuario.getRole().isBlank() || "USER".equals(usuario.getRole())) {
                    usuario.setRole("admin".equals(usuario.getUsername()) ? "ADMIN" : "ATENDENTE");
                    repo.save(usuario);
                }
            }
            criarSeAusente(repo, encoder, "admin", "teste123", "ADMIN");
            // Contas iniciais de demonstracao da atividade individual.
            criarSeAusente(repo, encoder, "gerente", "Gerente123", "GERENTE");
            criarSeAusente(repo, encoder, "atendente", "Atendente123", "ATENDENTE");
        };
    }
    private void criarSeAusente(UsuarioRepository repo, PasswordEncoder encoder, String login, String senha, String perfil) {
        if (repo.findByUsername(login).isPresent()) return;
        Usuario usuario = new Usuario(); usuario.setUsername(login); usuario.setPassword(encoder.encode(senha)); usuario.setRole(perfil);
        repo.save(usuario);
    }
}
