package ru.yandex.practicum.telemetry.handler.hub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioAddedEventProto;
import ru.yandex.practicum.telemetry.handler.HubEventHandler;

@Component
public class ScenarioAddedEventHandler implements HubEventHandler {

    private static final Logger log = LoggerFactory.getLogger(ScenarioAddedEventHandler.class);

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        ScenarioAddedEventProto added = event.getScenarioAdded();
        log.info("Хаб {}: добавлен сценарий id={}, conditions={}, actions={}",
                event.getHubId(), added.getId(),
                added.getConditionCount(), added.getActionCount());
    }
}