package ru.yandex.practicum.telemetry.analyzer.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.deserializer.HubEventDeserializer;
import ru.yandex.practicum.telemetry.analyzer.deserializer.SensorsSnapshotDeserializer;

import java.util.Properties;

@Configuration
public class KafkaConfig {

    @Bean(destroyMethod = "")
    public Consumer<String, SensorsSnapshotAvro> snapshotConsumer(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.snapshot.group-id}") String groupId,
            @Value("${spring.kafka.consumer.snapshot.auto-offset-reset}") String autoOffsetReset) {
        return buildConsumer(bootstrapServers, groupId, autoOffsetReset,
                SensorsSnapshotDeserializer.class.getName());
    }

    @Bean(destroyMethod = "")
    public Consumer<String, HubEventAvro> hubEventConsumer(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.hub-event.group-id}") String groupId,
            @Value("${spring.kafka.consumer.hub-event.auto-offset-reset}") String autoOffsetReset) {
        return buildConsumer(bootstrapServers, groupId, autoOffsetReset,
                HubEventDeserializer.class.getName());
    }

    private <T> Consumer<String, T> buildConsumer(String bootstrapServers,
                                                  String groupId,
                                                  String autoOffsetReset,
                                                  String valueDeserializer) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        return new KafkaConsumer<>(props);
    }
}