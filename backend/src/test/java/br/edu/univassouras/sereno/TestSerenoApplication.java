package br.edu.univassouras.sereno;

import org.springframework.boot.SpringApplication;

/** Sobe a aplicação com um Postgres descartável (Testcontainers), sem precisar do docker compose. */
public class TestSerenoApplication {

    public static void main(String[] args) {
        SpringApplication.from(SerenoApplication::main)
                .with(TestcontainersConfiguration.class)
                .withAdditionalProfiles("dev")
                .run(args);
    }
}
