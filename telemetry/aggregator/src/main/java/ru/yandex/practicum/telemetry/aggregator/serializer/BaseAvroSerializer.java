package ru.yandex.practicum.telemetry.aggregator.serializer;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BaseAvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {

    private final EncoderFactory encoderFactory = EncoderFactory.get();

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            DatumWriter<T> writer = new SpecificDatumWriter<>(data.getSchema());
            BinaryEncoder encoder = encoderFactory.binaryEncoder(out, null);
            writer.write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сериализации Avro-сообщения в топик " + topic, e);
        }
    }
}