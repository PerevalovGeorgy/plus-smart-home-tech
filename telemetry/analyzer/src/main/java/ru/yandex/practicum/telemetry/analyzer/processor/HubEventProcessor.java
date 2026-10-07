package ru.yandex.practicum.telemetry.analyzer.processor;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.entity.*;
import ru.yandex.practicum.telemetry.analyzer.repository.*;
import ru.yandex.practicum.telemetry.analyzer.service.HubEventService;

import java.time.Duration;
import java.util.List;

@Component
public class HubEventProcessor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HubEventProcessor.class);

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(1000);

    private final Consumer<String, HubEventAvro> consumer;
    private final HubEventService hubEventService;
    private final String topic;

    public HubEventProcessor(Consumer<String, HubEventAvro> consumer,
                             HubEventService hubEventService,
                             @Value("${kafka.topics.hubs}") String topic) {
        this.consumer = consumer;
        this.hubEventService = hubEventService;
        this.topic = topic;
    }

    @Override
    public void run() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            consumer.subscribe(List.of(topic));

            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(POLL_TIMEOUT);
                if (records.isEmpty()) {
                    continue;
                }

                boolean allProcessed = true;
                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    try {
                        hubEventService.processEvent(record.value());
                    } catch (Exception e) {
                        log.error("Ошибка при обработке события хаба: {}", record.value(), e);
                        allProcessed = false;
                    }
                }

                if (allProcessed) {
                    consumer.commitSync();
                } else {
                    log.warn("Не все сообщения из batch обработаны — offset не зафиксирован");
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка в HubEventProcessor", e);
        } finally {
            try {
                consumer.close();
            } catch (Exception e) {
                log.warn("Ошибка при закрытии консьюмера", e);
            }
        }
    }
}
