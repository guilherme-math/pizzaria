package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

// Reconsulta o perfil para que uma sessao antiga nao retenha privilegios revogados.
public class PerfilAtualFilter extends OncePerRequestFilter {
    private final UsuarioDetailsService usuarios;
    public PerfilAtualFilter(UsuarioDetailsService usuarios) { this.usuarios = usuarios; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            try {
                var atual = usuarios.loadUserByUsername(auth.getName());
                var atualizado = new UsernamePasswordAuthenticationToken(atual, null, atual.getAuthorities());
                atualizado.setDetails(auth.getDetails());
                SecurityContextHolder.getContext().setAuthentication(atualizado);
            } catch (UsernameNotFoundException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
            }
        }
        chain.doFilter(request, response);
    }
}
