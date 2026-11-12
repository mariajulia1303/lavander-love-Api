package com.floricultura.api.entity;

// niveis de acesso de uma chave de API, do mais fraco pro mais forte.
// a ordem importa: um nivel "permite" tudo que os niveis abaixo dele permitem.
public enum NivelAcesso {
    LEITURA,   // so consulta (GET)
    ESCRITA,   // consulta + cria/atualiza (GET, POST, PUT)
    ADMIN;     // tudo, inclusive DELETE e gerenciar chaves de API

    public boolean permite(NivelAcesso exigido) {
        return this.ordinal() >= exigido.ordinal();
    }
}
