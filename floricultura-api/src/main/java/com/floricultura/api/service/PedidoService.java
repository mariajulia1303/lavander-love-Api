package com.floricultura.api.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.floricultura.api.dto.PedidoRequest;
import com.floricultura.api.entity.Cliente;
import com.floricultura.api.entity.Pedido;
import com.floricultura.api.entity.Produto;
import com.floricultura.api.entity.StatusPedido;
import com.floricultura.api.exception.ConflictException;
import com.floricultura.api.exception.ResourceNotFoundException;
import com.floricultura.api.repository.ClienteRepository;
import com.floricultura.api.repository.PedidoRepository;
import com.floricultura.api.repository.ProdutoRepository;

// regras de negocio dos pedidos
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
    }

    public Pedido create(PedidoRequest req) {
        Pedido p = new Pedido();
        // resolve cliente e produtos; faltou algum id relacionado -> 409 (POST nunca da 404)
        p.setCliente(resolveCliente(req.getClienteId()));
        p.setProdutos(resolveProdutos(req.getProdutoIds()));
        p.setStatus(req.getStatus());
        p.setDataPedido(LocalDateTime.now());
        return pedidoRepository.save(p);
    }

    public Pedido findById(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido " + id + " nao encontrado"));
    }

    public Page<Pedido> findAll(Pageable pageable) {
        return pedidoRepository.findAll(pageable);
    }

    // consultas personalizadas
    public Page<Pedido> findByStatus(StatusPedido status, Pageable pageable) {
        return pedidoRepository.findByStatus(status, pageable);
    }

    public Page<Pedido> findByCliente(Long clienteId, Pageable pageable) {
        return pedidoRepository.findByClienteId(clienteId, pageable);
    }

    public Pedido update(Long id, PedidoRequest req) {
        Pedido p = findById(id);
        p.setCliente(resolveCliente(req.getClienteId()));
        p.setProdutos(resolveProdutos(req.getProdutoIds()));
        p.setStatus(req.getStatus());
        return pedidoRepository.save(p);
    }

    public void delete(Long id) {
        Pedido p = findById(id);
        pedidoRepository.delete(p);
    }

    private Cliente resolveCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ConflictException("Cliente " + clienteId + " nao existe"));
    }

    // resolve cada produto; se algum nao existir vira conflito (409)
    private List<Produto> resolveProdutos(List<Long> produtoIds) {
        List<Produto> produtos = new ArrayList<>();
        for (Long produtoId : produtoIds) {
            Produto produto = produtoRepository.findById(produtoId)
                    .orElseThrow(() -> new ConflictException("Produto " + produtoId + " nao existe"));
            produtos.add(produto);
        }
        return produtos;
    }
}
