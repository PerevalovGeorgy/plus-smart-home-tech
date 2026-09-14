package ru.yandex.practicum.telemetry.producer;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.config.EventTopics;

@Slf4j
@Component
@RequiredArgsConstructor

public class KafkaEventProducer {
    private final Producer<String, SpecificRecordBase> producer;
    private final EventTopics topics;

    public void sendSensorEvent(SensorEventAvro event) {
        send(topics.getSensors(), event.getHubId(), event);
    }

    public void sendHubEvent(HubEventAvro event) {
        log.debug(">>> sendHubEvent(): topic={}, key={}, event={}", topics.getHubs(), event.getHubId(), event);

        send(topics.getHubs(), event.getHubId(), event);
    }

    private void send(String topic, String key, SpecificRecordBase value) {
        log.debug(">>> send(): topic={}, key={}, value={}", topic, key, value);

        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(topic, key, value);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка отправки в Kafka: topic={}, key={}", topic, key, exception);
            } else {
                log.debug("Отправлено: topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    @PreDestroy
    public void close() {
        producer.flush();
        producer.close();
    }

}
