package com.mf.agentservice;

import com.mf.agentservice.config.MfAgentProperties;
import com.mf.agentservice.rag.RagProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({MfAgentProperties.class, RagProperties.class})
@EnableScheduling
public class MfAgentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MfAgentServiceApplication.class, args);
    }
}
