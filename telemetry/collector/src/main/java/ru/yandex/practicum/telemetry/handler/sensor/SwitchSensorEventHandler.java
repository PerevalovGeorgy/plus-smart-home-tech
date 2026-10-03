package ru.yandex.practicum.telemetry.handler.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SwitchSensorProto;
import ru.yandex.practicum.telemetry.handler.SensorEventHandler;

@Component
public class SwitchSensorEventHandler implements SensorEventHandler {

    private static final Logger log = LoggerFactory.getLogger(SwitchSensorEventHandler.class);

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.SWITCH;
    }

    @Override
    public void handle(SensorEventProto event) {
        SwitchSensorProto sw = event.getSwitch();
        log.info("Переключатель {}: state={}", event.getId(), sw.getState());
    }
}
