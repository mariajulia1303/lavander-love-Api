package com.floricultura.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;

// config do swagger, basicamente o cabecalho da doc da API
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI floriculturaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lavender Love")
                        .description("API RESTful da Lavander Love, uma floricultura. Projeto final do curso de APIs com Spring Boot. "
                                + "Representa o dia a dia de uma loja de flores: clientes, endereços, produtos, categorias e pedidos.\n\n"

                                + "**Regras gerais**\n\n"
                                + "- JSON em **UTF-8**; mensagens de erro em português (pt-BR).\n"
                                + "- Datas no formato ISO 8601 e valores em reais (BRL).\n"
                                + "- Erros: 404 não encontrado, 409 conflito, 422 validação.\n\n"

                                + "**Termos de uso (resumo)**\n\n"
                                + "- Envie dados verdadeiros e respeite a privacidade (LGPD).\n"
                                + "- Evite excesso de requisições, tentativas de invasão e conteúdo ilegal: "
                                + "o acesso pode ser **bloqueado**.\n"
                                + "- Prefira cancelar pedidos a apagá-los: exclusões não podem ser desfeitas.\n"
                                + "- Banco em memória: os dados são apagados ao reiniciar.\n"
                                + "- Uso intensivo em produção pode ter limites ou custos extras.")
                        .version("1.0")
                        .termsOfService("https://github.com/SEU-USUARIO/SEU-REPOSITORIO#termos-de-servi%C3%A7o")
                        .contact(new Contact()
                                .name("Lavander Love")
                                .email("contato@lavanderlove.com.br"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));

    }
}
