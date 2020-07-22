package com.github.faizal.libraryeventskafka.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

@Configuration
@EnableKafka
@Slf4j
public class ConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory concurrentKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactory factory){
        factory.setErrorHandler((thrownException, data) -> {
            log.info("Error {}. Data: {}",thrownException.getMessage(), data.value());
        });
        return factory;
    }
}
