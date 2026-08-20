package com.bte.credit_analysis_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Value("${app.http.ai-service.connect-timeout:10000}")
    private long connectTimeout;

    @Value("${app.http.ai-service.read-timeout:60000}")
    private long readTimeout;

    /**
     * RestTemplate avec timeout configuré pour appels au microservice IA
     * 
     * Timeout:
     * - Connection: 10 secondes
     * - Read: 60 secondes (attendre réponse LLM)
     */
    @Bean("aiServiceRestTemplate")
    public RestTemplate aiServiceRestTemplate() {
        ClientHttpRequestFactory requestFactory = new BufferingClientHttpRequestFactory(
            new SimpleClientHttpRequestFactory() {{
                setConnectTimeout((int) connectTimeout);
                setReadTimeout((int) readTimeout);
            }}
        );

        return new RestTemplate(requestFactory);
    }
}