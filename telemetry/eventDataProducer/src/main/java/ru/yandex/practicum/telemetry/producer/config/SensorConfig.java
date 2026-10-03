package ru.yandex.practicum.telemetry.producer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "sensor")
public class SensorConfig {
    private List<MotionSensorConfig> motionSensors = new ArrayList<>();
    private List<SwitchSensorConfig> switchSensors = new ArrayList<>();
    private List<TemperatureSensorConfig> temperatureSensors = new ArrayList<>();
    private List<LightSensorConfig> lightSensors = new ArrayList<>();
    private List<ClimateSensorConfig> climateSensors = new ArrayList<>();

    public List<MotionSensorConfig> getMotionSensors() { return motionSensors; }
    public void setMotionSensors(List<MotionSensorConfig> motionSensors) { this.motionSensors = motionSensors; }
    public List<SwitchSensorConfig> getSwitchSensors() { return switchSensors; }
    public void setSwitchSensors(List<SwitchSensorConfig> switchSensors) { this.switchSensors = switchSensors; }
    public List<TemperatureSensorConfig> getTemperatureSensors() { return temperatureSensors; }
    public void setTemperatureSensors(List<TemperatureSensorConfig> temperatureSensors) { this.temperatureSensors = temperatureSensors; }
    public List<LightSensorConfig> getLightSensors() { return lightSensors; }
    public void setLightSensors(List<LightSensorConfig> lightSensors) { this.lightSensors = lightSensors; }
    public List<ClimateSensorConfig> getClimateSensors() { return climateSensors; }
    public void setClimateSensors(List<ClimateSensorConfig> climateSensors) { this.climateSensors = climateSensors; }
}