package eu.cleankod.settlementtrigger.config;

import eu.cleankod.settlementtrigger.domain.EventOutcome;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.Map;

@Configuration
class KafkaConfig {

    @Bean
    ProducerFactory<String, EventOutcome> eventOutcomeProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class
        ));
    }

    @Bean
    KafkaTemplate<String, EventOutcome> eventOutcomeKafkaTemplate(
            ProducerFactory<String, EventOutcome> eventOutcomeProducerFactory) {
        return new KafkaTemplate<>(eventOutcomeProducerFactory);
    }
}
