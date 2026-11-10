package com.umc.pediupatrao.dashboard;

import java.time.*;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.umc.pediupatrao.entity.StatusPedido;

@Getter
@Setter
public class DashboardFiltro {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate inicio;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate fim;
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) private LocalTime horaInicio;
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) private LocalTime horaFim;
    private String regiao;
    private String clienteId;
    private String status;
    private String agrupamento = "DIA";
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    public Aplicado aplicar(Clock clock) {
        LocalDate hoje = LocalDate.now(clock.withZone(FUSO));
        LocalDate de = inicio == null ? hoje.minusDays(29) : inicio;
        LocalDate ate = fim == null ? hoje : fim;
        if (de.isAfter(ate) || ChronoUnit.DAYS.between(de, ate) > 365 || de.getYear() < 2000 || ate.getYear() > 2100) {
            throw invalido("Informe um periodo de ate 366 dias, com inicio anterior ou igual ao fim");
        }
        if ((horaInicio == null) != (horaFim == null)) throw invalido("Informe os dois horarios ou deixe ambos vazios");
        if (horaInicio != null && horaInicio.equals(horaFim)) throw invalido("Os horarios devem ser diferentes");
        if (!"DIA".equals(agrupamento) && !"MES".equals(agrupamento)) throw invalido("Agrupamento deve ser DIA ou MES");
        String estado = texto(status);
        if (estado != null) { try { StatusPedido.valueOf(estado); } catch (IllegalArgumentException e) { throw invalido("Status invalido"); } }
        String local = texto(regiao), cliente = texto(clienteId);
        if (local != null && local.length() > 250 || cliente != null && cliente.length() > 100) throw invalido("Filtro muito longo");
        return new Aplicado(de, ate, horaInicio, horaFim, local, cliente, estado, agrupamento);
    }
    private static String texto(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
    private static ResponseStatusException invalido(String motivo) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, motivo); }
    public record Aplicado(LocalDate inicio, LocalDate fim, LocalTime horaInicio, LocalTime horaFim,
            String regiao, String clienteId, String status, String agrupamento) {
        public boolean aceitaHora(LocalTime hora) {
            if (horaInicio == null) return true;
            return horaInicio.isBefore(horaFim) ? !hora.isBefore(horaInicio) && hora.isBefore(horaFim)
                : !hora.isBefore(horaInicio) || hora.isBefore(horaFim);
        }
        public boolean segmentado() { return horaInicio != null || regiao != null || clienteId != null || status != null; }
    }
}
