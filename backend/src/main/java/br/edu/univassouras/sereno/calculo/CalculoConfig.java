package br.edu.univassouras.sereno.calculo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class CalculoConfig {

    @Bean
    MotorCalculo motorCalculo() {
        return new MotorCalculo();
    }
}
