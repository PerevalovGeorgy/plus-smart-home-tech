package ru.yandex.practicum.telemetry.handler.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ClimateSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.telemetry.handler.SensorEventHandler;

@Component
public class ClimateSensorEventHandler implements SensorEventHandler {

    private static final Logger log = LoggerFactory.getLogger(ClimateSensorEventHandler.class);

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.CLIMATE;
    }

    @Override
    public void handle(SensorEventProto event) {
        ClimateSensorProto climate = event.getClimate();
        log.info("Климатический датчик {}: temperatureC={}, humidity={}, co2Level={}",
                event.getId(), climate.getTemperatureC(), climate.getHumidity(), climate.getCo2Level());
    }
}
