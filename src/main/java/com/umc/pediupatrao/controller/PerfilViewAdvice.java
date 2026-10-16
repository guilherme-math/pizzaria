package com.umc.pediupatrao.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PerfilViewAdvice {
    @ModelAttribute("perfilAtual")
    public String perfilAtual(Authentication authentication) {
        if (authentication == null) return "";
        return authentication.getAuthorities().stream().map(a -> a.getAuthority().replace("ROLE_", ""))
            .findFirst().orElse("");
    }
}
