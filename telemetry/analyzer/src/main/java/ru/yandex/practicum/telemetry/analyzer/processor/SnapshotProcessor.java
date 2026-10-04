package ru.yandex.practicum.telemetry.analyzer.processor;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.service.SnapshotService;

import java.time.Duration;
import java.util.List;

@Component
public class SnapshotProcessor {

    private static final Logger log = LoggerFactory.getLogger(SnapshotProcessor.class);
    public static final Duration POLL_TIMEOUT = Duration.ofMillis(1000);

    private final Consumer<String, SensorsSnapshotAvro> consumer;
    private final SnapshotService snapshotService;
    private final String topic;

    public SnapshotProcessor(Consumer<String, SensorsSnapshotAvro> consumer,
                              SnapshotService snapshotService,
                             @Value("${kafka.topics.snapshots}") String topic) {
        this.consumer = consumer;
        this.snapshotService = snapshotService;
        this.topic = topic;
    }

    public void start() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            consumer.subscribe(List.of(topic));

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(POLL_TIMEOUT);
                records.forEach(record -> {
                    try {
                        snapshotService.processSnapshot(record.value());   // ← через бин
                    } catch (Exception e) {
                        log.error("Ошибка обработки снапшота", e);
                    }
                });
                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка в SnapshotProcessor", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }
}