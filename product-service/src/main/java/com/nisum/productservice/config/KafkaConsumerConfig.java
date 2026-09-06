package com.nisum.productservice.config;

import com.nisum.productservice.event.ProductUpdatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<Long, ProductUpdatedEvent> consumerFactory() {

        Map<String, Object> config = new HashMap<>();

        config.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        config.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "product-cache-group"
        );

        return new DefaultKafkaConsumerFactory<>(
                config,
                new LongDeserializer(),
                new JacksonJsonDeserializer<>(ProductUpdatedEvent.class)
        );
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Long, ProductUpdatedEvent> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) -> {

                            System.out.println(
                                    "Publishing failed record to DLT: topic="
                                            + record.topic()
                                            + ", partition="
                                            + record.partition()
                                            + ", offset="
                                            + record.offset()
                            );

                            return new TopicPartition(
                                    record.topic() + ".DLT",
                                    record.partition()
                            );
                        }
                );

        FixedBackOff fixedBackOff = new FixedBackOff(
                2000L,
                3
        );

        return new DefaultErrorHandler(
                recoverer,
                fixedBackOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, ProductUpdatedEvent>
    kafkaListenerContainerFactory(DefaultErrorHandler kafkaErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<Long, ProductUpdatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler);

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, ProductUpdatedEvent>
    dltKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<Long, ProductUpdatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        FixedBackOff dltBackOff = new FixedBackOff(
                5000L,
                12
        );

        DefaultErrorHandler dltErrorHandler =
                new DefaultErrorHandler(dltBackOff);

        factory.setCommonErrorHandler(dltErrorHandler);

        return factory;
    }
}