package com.floricultura.api.security;

import com.floricultura.api.entity.NivelAcesso;
import com.floricultura.api.web.ApiPaths;

// regra unica de "qual nivel de chave cada rota exige". e usada pelo filtro de autenticacao
// E pela documentacao do Swagger, entao as duas coisas nunca ficam desencontradas.
//
//  GET  /produtos/**, /categorias/**      -> publico (catalogo da loja)
//  GET  /limites                           -> publico (politica de rate limit)
//  /auth/**                                -> publico (cadastro e login; usa X-Session-Token, nao X-API-Key)
//  GET  demais recursos                    -> LEITURA  (clientes/pedidos tem dado pessoal - LGPD)
//  POST / PUT                              -> ESCRITA
//  DELETE                                  -> ADMIN
//  /api-keys/**                            -> ADMIN    (exceto GET /api-keys/me: qualquer chave valida)
public final class PoliticaAcesso {

    private PoliticaAcesso() {
    }

    // devolve null quando a rota e publica
    public static NivelAcesso nivelExigido(String metodo, String caminho) {
        if (!ApiPaths.ehApi(caminho) || "OPTIONS".equalsIgnoreCase(metodo)) {
            return null;
        }
        if (ApiPaths.comecaCom(caminho, ApiPaths.CONTA)) {
            return null;
        }
        boolean get = "GET".equalsIgnoreCase(metodo) || "HEAD".equalsIgnoreCase(metodo);
        if (ApiPaths.comecaCom(caminho, java.util.List.of("/api-keys"))) {
            return (get && caminho.equals("/api-keys/me")) ? NivelAcesso.LEITURA : NivelAcesso.ADMIN;
        }
        if (get) {
            return ApiPaths.comecaCom(caminho, ApiPaths.PUBLICOS_GET) ? null : NivelAcesso.LEITURA;
        }
        if ("DELETE".equalsIgnoreCase(metodo)) {
            return NivelAcesso.ADMIN;
        }
        return NivelAcesso.ESCRITA;
    }
}
