package com.floricultura.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.floricultura.api.dto.ApiKeyCriadaResponse;
import com.floricultura.api.dto.ApiKeyRequest;
import com.floricultura.api.dto.ApiKeyUpdateRequest;
import com.floricultura.api.entity.ApiKey;
import com.floricultura.api.entity.NivelAcesso;
import com.floricultura.api.entity.Plano;
import com.floricultura.api.exception.ConflictException;
import com.floricultura.api.exception.ResourceNotFoundException;
import com.floricultura.api.repository.ApiKeyRepository;
import com.floricultura.api.repository.UsuarioRepository;

// regras das chaves de API: gerar, validar, atualizar, rotacionar e revogar
@Service
public class ApiKeyService {

    private static final String PREFIXO_CHAVE = "ll_";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository repository;
    private final UsuarioRepository usuarioRepository;

    public ApiKeyService(ApiKeyRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    // chave PESSOAL de quem fez login (usada pelo /auth/minha-chave):
    //  - ainda nao tem chave -> cria uma nova com o nivel e o plano padrao das contas
    //  - ja tem -> gera um valor novo pra MESMA chave (o antigo para de funcionar), assim cada pessoa
    //    continua com uma chave so
    //  - foi desativada por um ADMIN (ex: abuso) -> 409, a pessoa nao consegue reativar sozinha
    @Transactional
    public ApiKeyCriadaResponse gerarPessoal(ApiKey existente, String nomeDono, NivelAcesso nivel, Plano plano) {
        if (existente != null) {
            if (!existente.isAtiva()) {
                throw new ConflictException("Sua chave foi desativada por um administrador e nao pode ser renovada.");
            }
            return rotacionar(existente.getId());
        }
        String chavePura = novaChave();
        ApiKey apiKey = new ApiKey();
        String nome = "Chave pessoal de " + nomeDono;
        apiKey.setNome(nome.length() > 80 ? nome.substring(0, 80) : nome);
        apiKey.setNivel(nivel);
        apiKey.setPlano(plano);
        apiKey.setAtiva(true);
        aplicarChave(apiKey, chavePura);
        apiKey.setCriadaEm(LocalDateTime.now());
        return ApiKeyCriadaResponse.of(repository.save(apiKey), chavePura);
    }

    // gera uma chave aleatoria nova e devolve o valor puro UMA vez
    @Transactional
    public ApiKeyCriadaResponse gerar(ApiKeyRequest req) {
        String chavePura = novaChave();
        ApiKey apiKey = new ApiKey();
        apiKey.setNome(req.getNome());
        apiKey.setNivel(req.getNivel());
        apiKey.setPlano(req.getPlano() != null ? req.getPlano() : Plano.GRATUITO);
        apiKey.setAtiva(true);
        aplicarChave(apiKey, chavePura);
        apiKey.setCriadaEm(LocalDateTime.now());
        return ApiKeyCriadaResponse.of(repository.save(apiKey), chavePura);
    }

    // usado so na subida da aplicacao pra cadastrar as chaves de demonstracao do properties
    @Transactional
    public void cadastrarSeNaoExistir(String nome, String chavePura, NivelAcesso nivel, Plano plano) {
        if (chavePura == null || chavePura.isBlank() || repository.existsByHash(hash(chavePura))) {
            return;
        }
        ApiKey apiKey = new ApiKey();
        apiKey.setNome(nome);
        apiKey.setNivel(nivel);
        apiKey.setPlano(plano != null ? plano : Plano.GRATUITO);
        apiKey.setAtiva(true);
        aplicarChave(apiKey, chavePura);
        apiKey.setCriadaEm(LocalDateTime.now());
        repository.save(apiKey);
    }

    // valida a chave recebida no header: precisa existir e estar ativa
    @Transactional
    public Optional<ApiKey> validar(String chavePura) {
        if (chavePura == null || chavePura.isBlank()) {
            return Optional.empty();
        }
        Optional<ApiKey> encontrada = repository.findByHash(hash(chavePura.trim())).filter(ApiKey::isAtiva);
        encontrada.ifPresent(k -> {
            k.setUltimoUso(LocalDateTime.now());
            repository.save(k);
        });
        return encontrada;
    }

    public ApiKey findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chave de API " + id + " nao encontrada"));
    }

    public Page<ApiKey> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    // consulta personalizada: filtra por nivel e/ou se esta ativa
    public Page<ApiKey> search(NivelAcesso nivel, Boolean ativa, Pageable pageable) {
        if (nivel != null && ativa != null) {
            return repository.findByNivelAndAtiva(nivel, ativa, pageable);
        }
        if (nivel != null) {
            return repository.findByNivel(nivel, pageable);
        }
        return repository.findByAtiva(ativa, pageable);
    }

    @Transactional
    public ApiKey update(Long id, ApiKeyUpdateRequest req, ApiKey quemPediu) {
        ApiKey apiKey = findById(id);
        boolean perdeAdmin = apiKey.getNivel() == NivelAcesso.ADMIN
                && (req.getNivel() != NivelAcesso.ADMIN || !req.getAtiva());
        if (perdeAdmin && apiKey.isAtiva() && ehUltimoAdmin()) {
            throw new ConflictException("Nao e possivel rebaixar ou desativar a ultima chave ADMIN ativa");
        }
        apiKey.setNome(req.getNome());
        apiKey.setNivel(req.getNivel());
        apiKey.setAtiva(req.getAtiva());
        if (req.getPlano() != null) {
            apiKey.setPlano(req.getPlano());
        }
        return repository.save(apiKey);
    }

    // gera um valor novo pra mesma chave (o antigo para de funcionar na hora)
    @Transactional
    public ApiKeyCriadaResponse rotacionar(Long id) {
        ApiKey apiKey = findById(id);
        String chavePura = novaChave();
        aplicarChave(apiKey, chavePura);
        return ApiKeyCriadaResponse.of(repository.save(apiKey), chavePura);
    }

    @Transactional
    public void revogar(Long id, ApiKey quemPediu) {
        ApiKey apiKey = findById(id);
        if (quemPediu != null && quemPediu.getId().equals(apiKey.getId())) {
            throw new ConflictException("Nao e possivel revogar a propria chave que esta sendo usada na requisicao");
        }
        if (apiKey.getNivel() == NivelAcesso.ADMIN && apiKey.isAtiva() && ehUltimoAdmin()) {
            throw new ConflictException("Nao e possivel revogar a ultima chave ADMIN ativa");
        }
        // se a chave e pessoal de alguem, desliga do dono antes de apagar (senao o banco recusa)
        usuarioRepository.findByApiKeyId(apiKey.getId()).ifPresent(u -> {
            u.setApiKey(null);
            usuarioRepository.save(u);
        });
        repository.delete(apiKey);
    }

    private boolean ehUltimoAdmin() {
        return repository.countByNivelAndAtiva(NivelAcesso.ADMIN, true) <= 1;
    }

    private void aplicarChave(ApiKey apiKey, String chavePura) {
        apiKey.setHash(hash(chavePura));
        apiKey.setPrefixo(chavePura.substring(0, Math.min(8, chavePura.length())) + "...");
    }

    // "ll_" + 32 caracteres aleatorios (base64 url-safe)
    private static String novaChave() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return PREFIXO_CHAVE + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String valor) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
