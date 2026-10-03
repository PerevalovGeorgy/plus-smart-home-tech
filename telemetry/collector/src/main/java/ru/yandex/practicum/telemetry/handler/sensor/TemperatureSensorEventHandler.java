package ru.yandex.practicum.telemetry.handler.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.TemperatureSensorProto;
import ru.yandex.practicum.telemetry.handler.SensorEventHandler;

@Component
public class TemperatureSensorEventHandler implements SensorEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TemperatureSensorEventHandler.class);

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.TEMPERATURE;
    }

    @Override
    public void handle(SensorEventProto event) {
        TemperatureSensorProto temp = event.getTemperature();
        log.info("Датчик температуры {}: C={}, F={}",
                event.getId(), temp.getTemperatureC(), temp.getTemperatureF());
    }
}
