package com.opsflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class OpsFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpsFlowApplication.class, args);
    }
}
