package com.floricultura.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.floricultura.api.dto.EnderecoRequest;
import com.floricultura.api.entity.Endereco;
import com.floricultura.api.exception.ResourceNotFoundException;
import com.floricultura.api.repository.EnderecoRepository;

// regras de negocio dos enderecos
@Service
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;

    public EnderecoService(EnderecoRepository enderecoRepository) {
        this.enderecoRepository = enderecoRepository;
    }

    public Endereco create(EnderecoRequest req) {
        Endereco e = new Endereco();
        aplicar(e, req);
        return enderecoRepository.save(e);
    }

    public Endereco findById(Long id) {
        return enderecoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Endereco " + id + " nao encontrado"));
    }

    public Page<Endereco> findAll(Pageable pageable) {
        return enderecoRepository.findAll(pageable);
    }

    // consulta personalizada: enderecos por cidade
    public Page<Endereco> findByCidade(String cidade, Pageable pageable) {
        return enderecoRepository.findByCidadeIgnoreCase(cidade, pageable);
    }

    public Endereco update(Long id, EnderecoRequest req) {
        Endereco e = findById(id);
        aplicar(e, req);
        return enderecoRepository.save(e);
    }

    public void delete(Long id) {
        Endereco e = findById(id);
        enderecoRepository.delete(e);
    }

    // joga os campos do request pra dentro da entidade, pra nao repetir codigo
    private void aplicar(Endereco e, EnderecoRequest req) {
        e.setLogradouro(req.getLogradouro());
        e.setNumero(req.getNumero());
        e.setBairro(req.getBairro());
        e.setCidade(req.getCidade());
        e.setUf(req.getUf());
        e.setCep(req.getCep());
    }
}
