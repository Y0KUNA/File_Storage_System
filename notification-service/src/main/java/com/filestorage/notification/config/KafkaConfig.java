package com.filestorage.notification.config;


import com.filestorage.notification.config.*;
import com.filestorage.notification.controller.*;
import com.filestorage.notification.domain.*;
import com.filestorage.notification.dto.*;
import com.filestorage.notification.messaging.*;
import com.filestorage.notification.repository.*;
import com.filestorage.common.EventEnvelope;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventEnvelope> kafkaListenerContainerFactory(
            ConsumerFactory<String, EventEnvelope> consumerFactory,
            KafkaTemplate<String, EventEnvelope> kafkaTemplate,
            @Value("${app.kafka.dlq-suffix:.DLQ}") String dlqSuffix) {
        ConcurrentKafkaListenerContainerFactory<String, EventEnvelope> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception ex) -> new TopicPartition(record.topic() + dlqSuffix, record.partition()));
        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L)));
        return factory;
    }
}
