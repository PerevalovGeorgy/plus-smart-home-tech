package ru.yandex.practicum.telemetry.handler.hub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioRemovedEventProto;
import ru.yandex.practicum.telemetry.handler.HubEventHandler;

@Component
public class ScenarioRemovedEventHandler implements HubEventHandler {

    private static final Logger log = LoggerFactory.getLogger(ScenarioRemovedEventHandler.class);

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_REMOVED;
    }

    @Override
    public void handle(HubEventProto event) {
        ScenarioRemovedEventProto removed = event.getScenarioRemoved();
        log.info("Хаб {}: удалён сценарий id={}", event.getHubId(), removed.getId());
    }
}
