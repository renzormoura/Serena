package br.edu.univassouras.sereno;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SerenoApplication {

	public static void main(String[] args) {
		// SERENO_DB_URL pode vir no formato da plataforma (postgresql://usuario:senha@host/banco).
		UrlDoBanco.converter(System.getenv("SERENO_DB_URL"))
				.ifPresent(conexao -> UrlDoBanco.propriedades(conexao).forEach(System::setProperty));
		SpringApplication.run(SerenoApplication.class, args);
	}

}
