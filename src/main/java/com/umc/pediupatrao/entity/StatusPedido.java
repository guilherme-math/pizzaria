package com.umc.pediupatrao.entity;

public enum StatusPedido {
    RECEBIDO, EM_PREPARACAO, PRONTO, SAIU_PARA_ENTREGA, RETIRADO, FINALIZADO, CANCELADO;

    public boolean permite(StatusPedido proximo) {
        return switch (this) {
            case RECEBIDO -> proximo == EM_PREPARACAO;
            case EM_PREPARACAO -> proximo == PRONTO;
            case PRONTO -> proximo == SAIU_PARA_ENTREGA || proximo == RETIRADO;
            case SAIU_PARA_ENTREGA, RETIRADO -> proximo == FINALIZADO;
            default -> false;
        };
    }
    public boolean antesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }
}
