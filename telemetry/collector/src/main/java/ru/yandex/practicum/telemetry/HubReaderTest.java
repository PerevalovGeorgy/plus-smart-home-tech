package ru.yandex.practicum.telemetry;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.*;

public class HubReaderTest {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "hub-reader-test-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        int total = 0;
        try (KafkaConsumer<String, HubEventAvro> c = new KafkaConsumer<>(props)) {
            c.subscribe(List.of("telemetry.hubs.v1"));

            long deadline = System.currentTimeMillis() + 15_000;   // 15 секунд
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, HubEventAvro> records = c.poll(Duration.ofSeconds(3));
                for (ConsumerRecord<String, HubEventAvro> r : records) {
                    total++;
                    System.out.println("OK offset=" + r.offset() + " key=" + r.key() + " value=" + r.value());
                }
                if (total > 0 && records.isEmpty()) {
                    // прочитали всё, что было, и новых нет
                    break;
                }
            }
        }
        System.out.println("ИТОГО прочитано: " + total);
    }
}