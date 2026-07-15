package com.mf.agentservice.config;

import java.time.Duration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient mfEpRestClient(MfAgentProperties properties) {
        return restClient(properties.ep().baseUrl(), properties.agent().requestTimeout());
    }

    @Bean
    RestClient mfDataCenterRestClient(MfAgentProperties properties) {
        return restClient(properties.datacenter().baseUrl(), properties.agent().requestTimeout());
    }

    private RestClient restClient(String baseUrl, Duration timeout) {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(timeout)
                .withReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
