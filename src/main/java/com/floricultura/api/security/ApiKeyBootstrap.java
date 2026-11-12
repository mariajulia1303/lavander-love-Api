package com.floricultura.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.floricultura.api.entity.NivelAcesso;
import com.floricultura.api.entity.Plano;
import com.floricultura.api.service.ApiKeyService;

// na subida da aplicacao cadastra as chaves iniciais definidas no application.properties.
// sem isso ninguem conseguiria criar a primeira chave (criar chave exige chave ADMIN).
// em producao, troque os valores via variavel de ambiente (ex: LAVANDER_ADMIN_KEY).
@Component
public class ApiKeyBootstrap implements ApplicationRunner {

    private final ApiKeyService apiKeyService;

    @Value("${lavander.security.chave-admin:}")
    private String chaveAdmin;

    @Value("${lavander.security.chave-escrita:}")
    private String chaveEscrita;

    @Value("${lavander.security.chave-leitura:}")
    private String chaveLeitura;

    // plano das chaves de demonstracao. GRATUITO (o menor) de proposito: assim da pra ver o 429
    // clicando umas 10x seguidas no Swagger mesmo usando a chave ADMIN.
    @Value("${lavander.security.plano-chaves-demo:GRATUITO}")
    private Plano planoDemo;

    public ApiKeyBootstrap(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public void run(ApplicationArguments args) {
        apiKeyService.cadastrarSeNaoExistir("Chave inicial ADMIN", chaveAdmin, NivelAcesso.ADMIN, planoDemo);
        apiKeyService.cadastrarSeNaoExistir("Chave inicial ESCRITA", chaveEscrita, NivelAcesso.ESCRITA, planoDemo);
        apiKeyService.cadastrarSeNaoExistir("Chave inicial LEITURA", chaveLeitura, NivelAcesso.LEITURA, planoDemo);
    }
}
