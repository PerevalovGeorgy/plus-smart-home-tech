package ru.yandex.practicum.telemetry.dto.hub;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class ScenarioAddedEvent extends HubEvent{

    private String name;
    private List<ScenarioCondition> conditions;
    private List<DeviceAction> actions;


    @Override
    public HubEventType getPayload() {
        return HubEventType.SCENARIO_ADDED;
    }
}
