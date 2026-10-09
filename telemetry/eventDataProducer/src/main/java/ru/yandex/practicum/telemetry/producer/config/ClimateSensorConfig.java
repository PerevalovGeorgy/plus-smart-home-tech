package ru.yandex.practicum.telemetry.producer.config;

public class ClimateSensorConfig {
    private String id;
    private ValueRange temperature;
    private ValueRange humidity;
    private ValueRange co2Level;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ValueRange getTemperature() { return temperature; }
    public void setTemperature(ValueRange temperature) { this.temperature = temperature; }
    public ValueRange getHumidity() { return humidity; }
    public void setHumidity(ValueRange humidity) { this.humidity = humidity; }
    public ValueRange getCo2Level() { return co2Level; }
    public void setCo2Level(ValueRange co2Level) { this.co2Level = co2Level; }
}