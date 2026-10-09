package ru.yandex.practicum.telemetry.producer.config;

public class LightSensorConfig {
    private String id;
    private ValueRange luminosity;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ValueRange getLuminosity() { return luminosity; }
    public void setLuminosity(ValueRange luminosity) { this.luminosity = luminosity; }
}