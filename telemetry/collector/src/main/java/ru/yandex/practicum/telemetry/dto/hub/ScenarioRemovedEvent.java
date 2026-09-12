package ru.yandex.practicum.telemetry.dto.hub;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class ScenarioRemovedEvent extends HubEvent{

    private String name;

    @Override
    public HubEventType getPayload() {
        return HubEventType.SCENARIO_REMOVED;
    }
}
