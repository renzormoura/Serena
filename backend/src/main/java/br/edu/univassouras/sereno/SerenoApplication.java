package br.edu.univassouras.sereno;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SerenoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SerenoApplication.class, args);
	}

}
