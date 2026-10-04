package com.floricultura.api.dto;

import java.util.List;

import com.floricultura.api.entity.StatusPedido;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// corpo pra criar/atualizar pedido: cliente e produtos vem por id
public class PedidoRequest {

    @NotNull
    private Long clienteId;

    // precisa ter pelo menos um produto no pedido
    @NotEmpty
    private List<Long> produtoIds;

    @NotNull
    private StatusPedido status;

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public List<Long> getProdutoIds() {
        return produtoIds;
    }

    public void setProdutoIds(List<Long> produtoIds) {
        this.produtoIds = produtoIds;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }
}
