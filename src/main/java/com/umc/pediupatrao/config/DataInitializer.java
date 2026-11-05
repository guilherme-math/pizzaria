package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder encoder,
            @Value("${app.bootstrap-admin.username:}") String username,
            @Value("${app.bootstrap-admin.password:}") String password) {
        return args -> {
            List<Usuario> existentes = repo.findAll();
            // Migracao de USER nunca concede privilegios administrativos pelo nome do login.
            for (Usuario usuario : existentes) {
                if (usuario.getRole() == null || usuario.getRole().isBlank() || "USER".equals(usuario.getRole())) {
                    usuario.setRole("ATENDENTE");
                    repo.save(usuario);
                }
            }
            // Configuracao de bootstrap nunca redefine senha nem cria contas em banco ja administrado.
            if (existentes.stream().anyMatch(u -> "ADMIN".equals(u.getRole()))) return;
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                throw new IllegalStateException("Nenhum administrador cadastrado. Configure BOOTSTRAP_ADMIN_USERNAME e BOOTSTRAP_ADMIN_PASSWORD apenas para a primeira inicializacao.");
            }
            String login = username.trim();
            if (!login.matches("[A-Za-z0-9._-]{3,80}")) {
                throw new IllegalStateException("Login inicial deve ter de 3 a 80 caracteres: letras, numeros, ponto, hifen ou sublinhado.");
            }
            if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("Senha inicial deve ter pelo menos 12 caracteres e no maximo 72 bytes em UTF-8.");
            }
            if (repo.findByUsername(login).isPresent()) {
                throw new IllegalStateException("Login inicial ja pertence a uma conta existente. Escolha outro login. Nenhuma conta foi promovida ou redefinida.");
            }
            Usuario admin = new Usuario();
            admin.setUsername(login);
            admin.setPassword(encoder.encode(password));
            admin.setRole("ADMIN");
            repo.save(admin);
        };
    }
}
