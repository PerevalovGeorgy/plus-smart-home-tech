package ru.yandex.practicum.telemetry.producer.config;

public class MotionSensorConfig {
    private String id;
    private ValueRange linkQuality;
    private ValueRange voltage;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ValueRange getLinkQuality() { return linkQuality; }
    public void setLinkQuality(ValueRange linkQuality) { this.linkQuality = linkQuality; }
    public ValueRange getVoltage() { return voltage; }
    public void setVoltage(ValueRange voltage) { this.voltage = voltage; }
}