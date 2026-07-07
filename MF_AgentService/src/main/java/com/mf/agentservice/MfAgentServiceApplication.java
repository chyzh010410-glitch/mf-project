package com.mf.agentservice;

import com.mf.agentservice.config.MfAgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MfAgentProperties.class)
public class MfAgentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MfAgentServiceApplication.class, args);
    }
}
