package com.justjava.devFlow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.Files;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    //@Bean
    public IntegrationFlow fileIngestionFlow() {
        return IntegrationFlow
                .from(Files.inboundAdapter(new File("path/to/watch"))
                                .patternFilter("*.csv"),
                        c -> c.poller(Pollers.fixedDelay(5000)))
                .handle("flowableService", "startProcess")
                .get();
    }

}