package ru.yandex.practicum.telemetry.handler.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.LightSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.telemetry.handler.SensorEventHandler;

@Component
public class LightSensorEventHandler implements SensorEventHandler {

    private static final Logger log = LoggerFactory.getLogger(LightSensorEventHandler.class);

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.LIGHT;
    }

    @Override
    public void handle(SensorEventProto event) {
        LightSensorProto light = event.getLight();
        log.info("Датчик освещённости {}: luminosity={}, linkQuality={}",
                event.getId(), light.getLuminosity(), light.getLinkQuality());
    }
}
