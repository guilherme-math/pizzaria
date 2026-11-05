package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {
    @Mock UsuarioRepository repo;
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    DataInitializer initializer = new DataInitializer();
    String senha() { return UUID.randomUUID().toString(); }
    Usuario usuario(String login, String perfil) {
        Usuario u=new Usuario();u.setUsername(login);u.setRole(perfil);u.setPassword("hash-anterior");return u;
    }
    @Test void bancoComAdminIniciaSemSegredoENaoCriaContas() throws Exception {
        when(repo.findAll()).thenReturn(List.of(usuario("gestor","ADMIN")));
        initializer.initDatabase(repo,encoder,"","").run();
        verify(repo,never()).save(any());verify(repo,never()).findByUsername(anyString());
    }
    @Test void bancoComAdminIgnoraBootstrapSemRedefinirSenha() throws Exception {
        Usuario admin=usuario("gestor","ADMIN");when(repo.findAll()).thenReturn(List.of(admin));
        initializer.initDatabase(repo,encoder,"outro-login",senha()).run();
        assertEquals("hash-anterior",admin.getPassword());verify(repo,never()).save(any());
    }
    @Test void bancoVazioSemSegredoFalhaSemCriarCredenciais() {
        when(repo.findAll()).thenReturn(List.of());
        Exception e=assertThrows(IllegalStateException.class,()->initializer.initDatabase(repo,encoder,"","").run());
        assertTrue(e.getMessage().contains("BOOTSTRAP_ADMIN_PASSWORD"));verify(repo,never()).save(any());
    }
    @Test void rejeitaSenhaInicialCurta() {
        when(repo.findAll()).thenReturn(List.of());
        assertThrows(IllegalStateException.class,()->initializer.initDatabase(repo,encoder,"gestor","curta").run());
        verify(repo,never()).save(any());
    }
    @Test void criaSomenteAdminComSenhaDaConfiguracaoCriptografada() throws Exception {
        String segredo=senha();when(repo.findAll()).thenReturn(List.of());when(repo.findByUsername("gestor")).thenReturn(Optional.empty());
        initializer.initDatabase(repo,encoder,"gestor",segredo).run();
        ArgumentCaptor<Usuario> captor=ArgumentCaptor.forClass(Usuario.class);verify(repo,times(1)).save(captor.capture());
        Usuario salvo=captor.getValue();assertEquals("ADMIN",salvo.getRole());assertEquals("gestor",salvo.getUsername());
        assertNotEquals(segredo,salvo.getPassword());assertTrue(encoder.matches(segredo,salvo.getPassword()));
    }
    @Test void naoPromoveContaExistenteNemTrocaSenha() {
        Usuario u=usuario("gestor","ATENDENTE");when(repo.findAll()).thenReturn(List.of(u));when(repo.findByUsername("gestor")).thenReturn(Optional.of(u));
        assertThrows(IllegalStateException.class,()->initializer.initDatabase(repo,encoder,"gestor",senha()).run());
        assertEquals("ATENDENTE",u.getRole());assertEquals("hash-anterior",u.getPassword());verify(repo,never()).save(any());
    }
    @Test void loginAdminLegadoNaoGanhaPrivilegioPeloNome() throws Exception {
        Usuario legado=usuario("admin","USER");when(repo.findAll()).thenReturn(List.of(legado));when(repo.findByUsername("novo-gestor")).thenReturn(Optional.empty());
        initializer.initDatabase(repo,encoder,"novo-gestor",senha()).run();
        assertEquals("ATENDENTE",legado.getRole());assertEquals("hash-anterior",legado.getPassword());
        verify(repo).save(legado);
    }
    @Test void respeitaLimiteDeBytesDoBcryptSemExporSenhaNoErro() {
        when(repo.findAll()).thenReturn(List.of());String segredo="é".repeat(40);
        Exception e=assertThrows(IllegalStateException.class,()->initializer.initDatabase(repo,encoder,"gestor",segredo).run());
        assertFalse(e.getMessage().contains(segredo));verify(repo,never()).save(any());
    }
    @Test void rejeitaLoginForaDoFormato() {
        when(repo.findAll()).thenReturn(List.of());
        assertThrows(IllegalStateException.class,()->initializer.initDatabase(repo,encoder,"login com espacos",senha()).run());
        verify(repo,never()).save(any());
    }
}
