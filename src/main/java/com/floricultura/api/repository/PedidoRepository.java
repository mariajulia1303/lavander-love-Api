package com.floricultura.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.floricultura.api.entity.Pedido;
import com.floricultura.api.entity.StatusPedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // filtra pedidos por status (PENDENTE, PAGO, etc)
    Page<Pedido> findByStatus(StatusPedido status, Pageable pageable);

    // todos os pedidos de um cliente
    Page<Pedido> findByClienteId(Long clienteId, Pageable pageable);
}
