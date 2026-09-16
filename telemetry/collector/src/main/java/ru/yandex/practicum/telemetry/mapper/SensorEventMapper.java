package ru.yandex.practicum.telemetry.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.telemetry.dto.sensor.*;

import java.time.Instant;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
public class SensorEventMapper {

    public SensorEventAvro toAvro(SensorEvent dto) {
        Instant ts = resolveTimestamp(dto.getTimestamp());

        Object payload = switch (dto.getType()) {
            case LIGHT_SENSOR_EVENT       -> toLight((LightSensorEvent) dto);
            case CLIMATE_SENSOR_EVENT     -> toClimate((ClimateSensorEvent) dto);
            case MOTION_SENSOR_EVENT      -> toMotion((MotionSensorEvent) dto);
            case TEMPERATURE_SENSOR_EVENT -> toTemperature((TemperatureSensorEvent) dto);
            case SWITCH_SENSOR_EVENT      ->  toSwitch((SwitchSensorEvent) dto);
        };

        return SensorEventAvro.newBuilder()
                .setId(dto.getId())
                .setHubId(dto.getHubId())
                .setTimestamp(ts)
                .setPayload(payload)
                .build();
    }

    private LightSensorAvro toLight(LightSensorEvent dto) {
        return LightSensorAvro.newBuilder()
                .setLinkQuality(dto.getLinkQuality())
                .setLuminosity(dto.getLuminosity())
                .build();
    }

    private ClimateSensorAvro toClimate(ClimateSensorEvent dto) {
        return ClimateSensorAvro.newBuilder()
                .setTemperatureC(dto.getTemperatureC())
                .setHumidity(dto.getHumidity())
                .setCo2Level(dto.getCo2Level())
                .build();
    }

    private MotionSensorAvro toMotion(MotionSensorEvent dto) {
        return MotionSensorAvro.newBuilder()
                .setLinkQuality(dto.getLinkQuality())
                .setMotion(dto.isMotion())
                .setVoltage(dto.getVoltage())
                .build();
    }

    private SwitchSensorAvro toSwitch(SwitchSensorEvent dto) {
        return SwitchSensorAvro.newBuilder()
                .setState(dto.isState())
                .build();
    }

    private TemperatureSensorAvro toTemperature(TemperatureSensorEvent dto) {
        return TemperatureSensorAvro.newBuilder()
                .setTemperatureC(dto.getTemperatureC())
                .setTemperatureF(dto.getTemperatureF())
                .build();
    }

    private Instant resolveTimestamp(Instant ts) {
        return ts != null ? ts : Instant.now();
    }

}
