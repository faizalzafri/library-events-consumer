package com.github.faizal.libraryeventskafka.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;

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
        factory.setRetryTemplate(retryTemplate());
        factory.setRecoveryCallback(context -> {
            if(context.getLastThrowable().getCause() instanceof RecoverableDataAccessException){
                log.info("Recovering ");
            }else{
                log.info("Not recovering ");
//                context.setExhaustedOnly();
                throw new RuntimeException(context.getLastThrowable().getMessage());
            }
//            return context;
            return null;
        });

        return factory;
    }

    // returs a retry template
    private RetryTemplate retryTemplate() {

        // back of 1000ms before retry
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(1000);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(simpleRetryPolicy());
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        return retryTemplate;
    }

    private RetryPolicy simpleRetryPolicy() {
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(3);
        return simpleRetryPolicy;
    }

    private RetryPolicy exceptionWiseRetryPolicy() {
        HashMap<Class<? extends Throwable>, Boolean> map = new HashMap<>();
        map.put(IllegalArgumentException.class, false);
        map.put(RecoverableDataAccessException.class, true);
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(3, map, true);
        return simpleRetryPolicy;
    }
}
