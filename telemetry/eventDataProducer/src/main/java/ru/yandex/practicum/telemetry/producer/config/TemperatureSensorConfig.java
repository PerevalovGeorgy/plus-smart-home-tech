package ru.yandex.practicum.telemetry.producer.config;

public class TemperatureSensorConfig {
    private String id;
    private ValueRange temperature;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ValueRange getTemperature() { return temperature; }
    public void setTemperature(ValueRange temperature) { this.temperature = temperature; }
}