package com.floricultura.api.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import jakarta.servlet.http.HttpServletRequest;

import com.floricultura.api.web.FilterOrder;

// politica de CORS da API.
// o navegador bloqueia um front-end (ex: http://127.0.0.1:5500 do Live Server) de chamar a API
// em outra origem (http://localhost:8080) a menos que a API diga quem pode. aqui liberamos
// SO as origens configuradas, SO os metodos usados e SO os headers que a API entende.
//
// usamos um CorsFilter (e nao so @CrossOrigin/WebMvcConfigurer) porque ele roda antes dos
// filtros de chave de API e rate limit: assim o preflight OPTIONS nao toma 401 e as respostas
// 401/403/429 tambem saem com os headers de CORS (senao o front nem consegue ler o erro).
@Configuration
public class CorsConfig {

    @Value("${lavander.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "X-API-Key", "X-API-Version",
                "X-Idempotency-Key", "X-Session-Token"));
        // headers de resposta que o JavaScript do front pode ler
        config.setExposedHeaders(List.of("Location", "X-API-Version", "X-RateLimit-Limit",
                "X-RateLimit-Remaining", "X-RateLimit-Reset", "X-RateLimit-Policy", "Retry-After", "X-Idempotency-Key",
                "X-Idempotency-Replayed"));
        config.setAllowCredentials(false);
        // o navegador pode guardar o resultado do preflight por 1h
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsMesmaOrigemFilter(source));
        bean.setOrder(FilterOrder.CORS);
        return bean;
    }

    // quando o front e aberto pela propria API (ex.: https://lavander-love-api.onrender.com/), o navegador
    // manda o header Origin no POST do login mesmo sendo o MESMO endereco. atras do proxy do Render a
    // requisicao chega como http e em outra porta, entao o CorsFilter achava que era outra origem e
    // respondia 403 "Invalid CORS request". aqui: se a Origin e o proprio endereco publico da API
    // (montado com os headers X-Forwarded-* que o Render envia), nao e CORS e o filtro nao se aplica.
    static class CorsMesmaOrigemFilter extends CorsFilter {

        CorsMesmaOrigemFilter(UrlBasedCorsConfigurationSource source) {
            super(source);
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String origin = request.getHeader("Origin");
            if (origin == null) {
                return false;
            }
            String proto = primeiro(request.getHeader("X-Forwarded-Proto"), request.getScheme());
            String host = primeiro(request.getHeader("X-Forwarded-Host"), request.getHeader("Host"));
            if (host == null) {
                return false;
            }
            String propria = proto + "://" + host;
            return origin.equalsIgnoreCase(propria)
                    || origin.equalsIgnoreCase(propria.replace(":443", "").replace(":80", ""));
        }

        // X-Forwarded-* pode vir como "https,http" quando passa por mais de um proxy: vale o primeiro
        private static String primeiro(String encaminhado, String padrao) {
            if (encaminhado == null || encaminhado.isBlank()) {
                return padrao;
            }
            return encaminhado.split(",")[0].trim();
        }
    }
}
