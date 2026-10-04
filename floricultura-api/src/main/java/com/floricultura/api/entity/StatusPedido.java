package com.floricultura.api.entity;

// estados pelos quais um pedido pode passar
public enum StatusPedido {
    PENDENTE,
    PAGO,
    EM_PREPARACAO,
    ENVIADO,
    ENTREGUE,
    CANCELADO
}
