package com.bte.credit_analysis_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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
        // Créer la factory avec timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout);
        factory.setReadTimeout((int) readTimeout);

        // Wrapper pour buffering
        ClientHttpRequestFactory bufferingFactory = 
            new BufferingClientHttpRequestFactory(factory);

        // Construire RestClient avec la factory
        return RestClient.builder()
            .requestFactory(bufferingFactory)
            .build();
    }
}