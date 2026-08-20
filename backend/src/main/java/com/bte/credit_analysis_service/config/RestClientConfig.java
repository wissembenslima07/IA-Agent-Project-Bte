package com.bte.credit_analysis_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${app.http.ai-service.connect-timeout:10000}")
    private long connectTimeout;

    @Value("${app.http.ai-service.read-timeout:60000}")
    private long readTimeout;

    /**
     * RestClient bean avec timeout configuré pour appels au microservice IA
     * 
     * Timeout:
     * - Connection: 10 secondes
     * - Read: 60 secondes (attendre réponse LLM)
     */
    @Bean
    public RestClient restClient() {
        // Client HTTP JDK (java.net.http.HttpClient) : plus fiable que
        // SimpleClientHttpRequestFactory pour les réponses longues (LLM),
        // qui pouvait perdre l'en-tête Content-Type sur les requêtes lentes.
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(connectTimeout))
            .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        // Wrapper pour buffering
        ClientHttpRequestFactory bufferingFactory =
            new BufferingClientHttpRequestFactory(factory);

        // Construire RestClient avec la factory
        return RestClient.builder()
            .requestFactory(bufferingFactory)
            .build();
    }
}