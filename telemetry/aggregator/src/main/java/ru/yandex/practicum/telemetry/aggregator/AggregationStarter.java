package ru.yandex.practicum.telemetry.aggregator;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AggregationStarter {

    private static final Logger log = LoggerFactory.getLogger(AggregationStarter.class);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(1000);

    private final Consumer<String, SensorEventAvro> consumer;
    private final Producer<String, SensorsSnapshotAvro> producer;
    private final String sensorsTopic;
    private final String snapshotsTopic;

    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public AggregationStarter(
            Consumer<String, SensorEventAvro> consumer,
            Producer<String, SensorsSnapshotAvro> producer,
            @Value("${kafka.topics.sensors}") String sensorsTopic,
            @Value("${kafka.topics.snapshots}") String snapshotsTopic) {
        this.consumer = consumer;
        this.producer = producer;
        this.sensorsTopic = sensorsTopic;
        this.snapshotsTopic = snapshotsTopic;
    }

    public void start() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            consumer.subscribe(List.of(sensorsTopic));

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(POLL_TIMEOUT);
                if (records.isEmpty()) {
                    continue;
                }

                boolean allProcessed = true;
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    try {
                        updateState(record.value()).ifPresent(snapshot -> {
                            producer.send(new ProducerRecord<>(
                                    snapshotsTopic,
                                    snapshot.getHubId(),
                                    snapshot
                            ));
                            log.info("Отправлен снапшот для хаба {}", snapshot.getHubId());
                        });
                    } catch (Exception e) {
                        log.error("Ошибка обработки события датчика {}", record.value(), e);
                        allProcessed = false;
                    }
                }

                if (allProcessed) {
                    consumer.commitSync();
                } else {
                    log.warn("Не все события датчиков из batch обработаны — offset не зафиксирован");
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                producer.flush();
            } catch (Exception e) {
                log.warn("Ошибка при flush продюсера", e);
            } finally {
                try {
                    consumer.close();
                } catch (Exception e) {
                    log.warn("Ошибка при закрытии консьюмера", e);
                }
                try {
                    producer.close();
                } catch (Exception e) {
                    log.warn("Ошибка при закрытии продюсера", e);
                }
            }
        }
    }

    private Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(hubId, id ->
                SensorsSnapshotAvro.newBuilder()
                        .setHubId(id)
                        .setTimestamp(event.getTimestamp())
                        .setSensorsState(new HashMap<>())
                        .build());

        String sensorId = event.getId();
        SensorStateAvro oldState = snapshot.getSensorsState().get(sensorId);

        if (oldState != null) {
            if (oldState.getTimestamp().isAfter(event.getTimestamp())
                    || oldState.getData().equals(event.getPayload())) {
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        snapshot.getSensorsState().put(sensorId, newState);
        snapshot.setTimestamp(event.getTimestamp());

        return Optional.of(snapshot);
    }
}