// Injeta o logo da Floricultura logo acima do cabecalho do Swagger UI.
// Carregado via springdoc.swagger-ui.custom-js-url. O Swagger demora um tiquinho
// pra montar a .info na tela, entao a gente fica tentando ate ela aparecer.
(function () {
    function inserirLogo() {
        var info = document.querySelector('.swagger-ui .info');
        if (!info) {
            return false;
        }
        // nao duplica se o script rodar de novo
        if (document.querySelector('.floricultura-logo')) {
            return true;
        }
        var header = document.createElement('div');
        header.className = 'floricultura-logo';

        var img = document.createElement('img');
        img.src = '/swagger-theme/logo.svg';
        img.alt = 'Floricultura';

        var span = document.createElement('span');
        span.textContent = 'Floricultura API';

        header.appendChild(img);
        header.appendChild(span);

        // coloca o bloco logo antes da secao de info (titulo/descricao)
        info.parentNode.insertBefore(header, info);
        return true;
    }

    var tentativas = 0;
    var timer = setInterval(function () {
        tentativas++;
        if (inserirLogo() || tentativas > 50) {
            clearInterval(timer);
        }
    }, 200);
})();
