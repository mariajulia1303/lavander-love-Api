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

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(FilterOrder.CORS);
        return bean;
    }
}
