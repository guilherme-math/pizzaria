package com.umc.pediupatrao.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import java.util.Map;

@ControllerAdvice
public class ErroAdvice {
    @ExceptionHandler(ResponseStatusException.class)
    public Object regra(ResponseStatusException erro, HttpServletRequest request) {
        return resposta(erro.getStatusCode().value(), erro.getReason(), request);
    }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public Object concorrencia(OptimisticLockingFailureException erro, HttpServletRequest request) {
        return resposta(409, "Registro alterado por outra operacao. Atualize a pagina e tente novamente.", request);
    }
    @ExceptionHandler(DataAccessException.class)
    public Object banco(DataAccessException erro, HttpServletRequest request) {
        return resposta(503, "Nao foi possivel concluir a operacao no banco. Atualize a pagina e confira os dados antes de tentar novamente.", request);
    }
    private Object resposta(int status, String mensagem, HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.status(status).body(Map.of("message", mensagem == null ? "Operacao rejeitada" : mensagem));
        }
        ModelAndView view = new ModelAndView("error");
        view.setStatus(HttpStatusCode.valueOf(status));
        view.addObject("status", status); view.addObject("message", mensagem);
        return view;
    }
}
