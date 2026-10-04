package com.floricultura.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.floricultura.api.dto.ClienteRequest;
import com.floricultura.api.dto.EnderecoRequest;
import com.floricultura.api.entity.Cliente;
import com.floricultura.api.entity.Endereco;
import com.floricultura.api.exception.ConflictException;
import com.floricultura.api.exception.ResourceNotFoundException;
import com.floricultura.api.repository.ClienteRepository;

// regras de negocio dos clientes
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // cria cliente; email duplicado vira 409
    public Cliente create(ClienteRequest req) {
        if (clienteRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new ConflictException("Ja existe um cliente com o email '" + req.getEmail() + "'");
        }
        Cliente c = new Cliente();
        c.setNome(req.getNome());
        c.setEmail(req.getEmail());
        c.setTelefone(req.getTelefone());
        // endereco salva junto por causa do cascade ALL
        c.setEndereco(montarEndereco(new Endereco(), req.getEndereco()));
        return clienteRepository.save(c);
    }

    public Cliente findById(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente " + id + " nao encontrado"));
    }

    public Page<Cliente> findAll(Pageable pageable) {
        return clienteRepository.findAll(pageable);
    }

    // consulta personalizada: busca por nome
    public Page<Cliente> search(String nome, Pageable pageable) {
        return clienteRepository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    public Cliente update(Long id, ClienteRequest req) {
        Cliente c = findById(id);
        // trocou de email pra um que ja e de outro cliente? 409
        if (!c.getEmail().equals(req.getEmail())
                && clienteRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new ConflictException("Ja existe um cliente com o email '" + req.getEmail() + "'");
        }
        c.setNome(req.getNome());
        c.setEmail(req.getEmail());
        c.setTelefone(req.getTelefone());
        // reaproveita o endereco que ja existe, so atualiza os campos
        Endereco endereco = c.getEndereco() != null ? c.getEndereco() : new Endereco();
        c.setEndereco(montarEndereco(endereco, req.getEndereco()));
        return clienteRepository.save(c);
    }

    public void delete(Long id) {
        Cliente c = findById(id);
        clienteRepository.delete(c);
    }

    private Endereco montarEndereco(Endereco e, EnderecoRequest req) {
        e.setLogradouro(req.getLogradouro());
        e.setNumero(req.getNumero());
        e.setBairro(req.getBairro());
        e.setCidade(req.getCidade());
        e.setUf(req.getUf());
        e.setCep(req.getCep());
        return e;
    }
}
