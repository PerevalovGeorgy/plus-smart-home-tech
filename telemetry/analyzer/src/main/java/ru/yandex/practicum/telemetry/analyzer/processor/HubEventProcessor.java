package ru.yandex.practicum.telemetry.analyzer.processor;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.entity.*;
import ru.yandex.practicum.telemetry.analyzer.entity.enums.ActionType;
import ru.yandex.practicum.telemetry.analyzer.entity.enums.ConditionOperation;
import ru.yandex.practicum.telemetry.analyzer.entity.enums.ConditionType;
import ru.yandex.practicum.telemetry.analyzer.repository.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class HubEventProcessor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HubEventProcessor.class);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(1000);

    private final Consumer<String, HubEventAvro> consumer;
    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final String topic;

    public HubEventProcessor(Consumer<String, HubEventAvro> consumer,
                             SensorRepository sensorRepository,
                             ScenarioRepository scenarioRepository,
                             ConditionRepository conditionRepository,
                             ActionRepository actionRepository,
                             @Value("${kafka.topics.hubs}") String topic) {
        this.consumer = consumer;
        this.sensorRepository = sensorRepository;
        this.scenarioRepository = scenarioRepository;
        this.conditionRepository = conditionRepository;
        this.actionRepository = actionRepository;
        this.topic = topic;
    }

    @Override
    public void run() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            consumer.subscribe(List.of(topic));

            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(POLL_TIMEOUT);
                records.forEach(record -> {
                    try {
                        processEvent(record.value());
                    } catch (Exception e) {
                        log.error("Ошибка при обработке события хаба: {}", record.value(), e);
                    }
                });
                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
            // штатное завершение
        } catch (Exception e) {
            log.error("Ошибка в HubEventProcessor", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }

    @Transactional
    protected void processEvent(HubEventAvro event) {
        String hubId = event.getHubId();
        Object payload = event.getPayload();

        if (payload instanceof DeviceAddedEventAvro added) {
            sensorRepository.findById(added.getId()).ifPresentOrElse(
                    s -> log.info("Устройство {} уже существует", added.getId()),
                    () -> {
                        Sensor sensor = new Sensor();
                        sensor.setId(added.getId());
                        sensor.setHubId(hubId);
                        sensorRepository.save(sensor);
                        log.info("Добавлено устройство {}", added.getId());
                    });
        } else if (payload instanceof DeviceRemovedEventAvro removed) {
            sensorRepository.findById(removed.getId())
                    .ifPresent(s -> {
                        sensorRepository.delete(s);
                        log.info("Удалено устройство {}", removed.getId());
                    });
        } else if (payload instanceof ScenarioAddedEventAvro added) {
            saveScenario(hubId, added);
        } else if (payload instanceof ScenarioRemovedEventAvro removed) {
            scenarioRepository.findByHubIdAndName(hubId, removed.getName())
                    .ifPresent(s -> {
                        scenarioRepository.delete(s);
                        log.info("Удалён сценарий {}", removed.getName());
                    });
        } else {
            log.warn("Неизвестный тип события хаба: {}", payload.getClass());
        }
    }

    private void saveScenario(String hubId, ScenarioAddedEventAvro event) {
        scenarioRepository.findByHubIdAndName(hubId, event.getName())
                .ifPresent(scenarioRepository::delete);

        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(event.getName());

        for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
            Optional<Sensor> sensor = sensorRepository.findByIdAndHubId(
                    conditionAvro.getSensorId(), hubId);
            if (sensor.isEmpty()) {
                log.warn("Датчик {} не найден для хаба {}", conditionAvro.getSensorId(), hubId);
                continue;
            }

            Condition condition = new Condition();
            condition.setType(ConditionType.valueOf(conditionAvro.getType().name()));
            condition.setOperation(ConditionOperation.valueOf(conditionAvro.getOperation().name()));
            condition.setValue(extractIntValue(conditionAvro.getValue()));

            ScenarioCondition link = new ScenarioCondition();
            link.setScenario(scenario);
            link.setSensor(sensor.get());
            link.setCondition(condition);
            scenario.getConditions().add(link);
        }

        for (DeviceActionAvro actionAvro : event.getActions()) {
            Optional<Sensor> sensor = sensorRepository.findByIdAndHubId(
                    actionAvro.getSensorId(), hubId);
            if (sensor.isEmpty()) {
                log.warn("Датчик {} не найден для хаба {}", actionAvro.getSensorId(), hubId);
                continue;
            }

            Action action = new Action();
            action.setType(ActionType.valueOf(actionAvro.getType().name()));
            action.setValue(actionAvro.getValue());

            ScenarioAction link = new ScenarioAction();
            link.setScenario(scenario);
            link.setSensor(sensor.get());
            link.setAction(action);
            scenario.getActions().add(link);
        }

        scenarioRepository.save(scenario);
        log.info("Сохранён сценарий {}", event.getName());
    }

    private Integer extractIntValue(Object value) {
        if (value instanceof Integer i) return i;
        if (value instanceof Boolean b) return b ? 1 : 0;
        return null;
    }
}