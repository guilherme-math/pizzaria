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
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ErroAdvice.class);
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
        if (request.getRequestURI().startsWith("/api/dashboard")) {
            Throwable causa = erro.getMostSpecificCause();
            Integer codigo = null;
            for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
                if (atual instanceof com.mongodb.MongoException mongo) {
                    codigo = mongo.getCode();
                    break;
                }
            }
            LOG.warn("Falha no banco do dashboard: operacao={}, erro={}, causa={}, codigoMongo={}",
                request.getMethod(), erro.getClass().getSimpleName(), causa.getClass().getSimpleName(), codigo);
        }
        return resposta(503, "Nao foi possivel concluir a operacao no banco. Atualize a pagina e confira os dados antes de tentar novamente.", request);
    }
    @ExceptionHandler({org.springframework.validation.BindException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class})
    public Object entradaInvalida(Exception erro, HttpServletRequest request) {
        return resposta(400, "Parametros invalidos. Use datas AAAA-MM-DD, horarios HH:mm e valores numericos validos.", request);
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
