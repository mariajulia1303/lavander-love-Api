package com.floricultura.api.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floricultura.api.dto.ApiKeyCriadaResponse;
import com.floricultura.api.dto.CadastroRequest;
import com.floricultura.api.dto.LoginRequest;
import com.floricultura.api.dto.LoginResponse;
import com.floricultura.api.dto.UsuarioResponse;
import com.floricultura.api.entity.NivelAcesso;
import com.floricultura.api.entity.Plano;
import com.floricultura.api.entity.Usuario;
import com.floricultura.api.exception.ConflictException;
import com.floricultura.api.exception.ResourceNotFoundException;
import com.floricultura.api.exception.UnauthorizedException;
import com.floricultura.api.repository.UsuarioRepository;
import com.floricultura.api.security.SenhaUtil;
import com.floricultura.api.security.SessaoService;

// cadastro, login e chave de API pessoal
@Service
public class UsuarioService {

    // mesma mensagem pra e-mail inexistente e senha errada: assim ninguem descobre quais e-mails tem conta
    private static final String LOGIN_INVALIDO = "E-mail ou senha incorretos.";

    private final UsuarioRepository repository;
    private final SessaoService sessaoService;
    private final ApiKeyService apiKeyService;

    // nivel e plano da chave que cada conta recebe (o ADMIN pode mudar depois em PUT /api-keys/{id})
    @Value("${lavander.auth.nivel-chave-pessoal:ESCRITA}")
    private NivelAcesso nivelChavePessoal;

    @Value("${lavander.auth.plano-chave-pessoal:BASICO}")
    private Plano planoChavePessoal;

    public UsuarioService(UsuarioRepository repository, SessaoService sessaoService, ApiKeyService apiKeyService) {
        this.repository = repository;
        this.sessaoService = sessaoService;
        this.apiKeyService = apiKeyService;
    }

    @Transactional
    public UsuarioResponse cadastrar(CadastroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ja existe uma conta com este e-mail.");
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(SenhaUtil.gerarHash(req.senha()));
        u.setCriadoEm(LocalDateTime.now());
        return UsuarioResponse.from(repository.save(u));
    }

    public LoginResponse login(LoginRequest req) {
        Usuario u = repository.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new UnauthorizedException(LOGIN_INVALIDO));
        if (!SenhaUtil.confere(req.senha(), u.getSenhaHash())) {
            throw new UnauthorizedException(LOGIN_INVALIDO);
        }
        SessaoService.Sessao s = sessaoService.abrir(u.getId());
        return new LoginResponse(s.token(), s.expiraEm(), UsuarioResponse.from(u));
    }

    public UsuarioResponse minhaConta(String token) {
        return UsuarioResponse.from(usuarioDaSessao(token));
    }

    // gera a chave pessoal (ou um valor novo pra ela). o valor aparece SO nesta resposta.
    @Transactional
    public ApiKeyCriadaResponse gerarMinhaChave(String token) {
        Usuario u = usuarioDaSessao(token);
        ApiKeyCriadaResponse criada = apiKeyService.gerarPessoal(u.getApiKey(), u.getNome(),
                nivelChavePessoal, planoChavePessoal);
        if (u.getApiKey() == null) {
            u.setApiKey(apiKeyService.findById(criada.getId()));
            repository.save(u);
        }
        return criada;
    }

    // exclui a chave pessoal: o valor para de funcionar na hora e a conta fica sem chave
    // (da pra criar outra depois em POST /auth/minha-chave)
    @Transactional
    public void excluirMinhaChave(String token) {
        Usuario u = usuarioDaSessao(token);
        if (u.getApiKey() == null) {
            throw new ResourceNotFoundException("Voce ainda nao tem chave de API para excluir.");
        }
        apiKeyService.revogar(u.getApiKey().getId(), null);
    }

    public void logout(String token) {
        sessaoService.fechar(token);
    }

    private Usuario usuarioDaSessao(String token) {
        SessaoService.Sessao s = sessaoService.buscar(token)
                .orElseThrow(() -> new UnauthorizedException(
                        "Sessao ausente ou expirada. Faca login de novo e envie o token no header X-Session-Token."));
        return repository.findById(s.usuarioId())
                .orElseThrow(() -> new UnauthorizedException("Conta nao encontrada. Faca login de novo."));
    }
}
