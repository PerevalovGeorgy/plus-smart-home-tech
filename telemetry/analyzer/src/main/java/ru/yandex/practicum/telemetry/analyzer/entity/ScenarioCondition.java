package ru.yandex.practicum.telemetry.analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "scenario_conditions")
@IdClass(ScenarioCondition.Pk.class)
@Getter
@Setter
public class ScenarioCondition {

    @Id
    @ManyToOne
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    @Id
    @ManyToOne
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @Id
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "condition_id")
    private Condition condition;

    public static class Pk implements Serializable {
        private Long scenario;
        private String sensor;
        private Long condition;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(scenario, pk.scenario)
                    && Objects.equals(sensor, pk.sensor)
                    && Objects.equals(condition, pk.condition);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scenario, sensor, condition);
        }
    }
}