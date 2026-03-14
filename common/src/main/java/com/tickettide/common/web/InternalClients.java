package com.tickettide.common.web;

import com.tickettide.common.security.InternalApiKeyFilter;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Builds RestClients for /internal/** calls with the shared key and sane timeouts. */
public final class InternalClients {

    private InternalClients() {
    }

    public static RestClient create(RestClient.Builder builder, String baseUrl, String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(InternalApiKeyFilter.HEADER, apiKey)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
