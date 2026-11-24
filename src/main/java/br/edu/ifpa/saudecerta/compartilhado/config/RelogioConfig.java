package br.edu.ifpa.saudecerta.compartilhado.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O "agora" do sistema e um bean. Assim as regras que dependem de data
 * (ex.: nao agendar no passado) podem ser testadas com um relogio fixo.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.systemDefaultZone();
    }
}
