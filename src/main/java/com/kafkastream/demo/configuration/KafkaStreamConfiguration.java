package com.kafkastream.demo.configuration;

import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.kafkastreams.events.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class KafkaStreamConfiguration {

    @Bean
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, GenericAvroSerde.class);
        props.put("schema.registry.url","http://localhost:8081");
      //  props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG,"kafka-streams-demo");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG , "earliest");
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public StreamsBuilderFactoryBean kStreamBuilder() {
        return new StreamsBuilderFactoryBean(kStreamsConfig());
    }

    @Bean
    public KStream<String, User> kStream(StreamsBuilder kStreamBuilder) {
        final Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url",
                "http://localhost:8081");
        final Serde<User> valueSpecificAvroSerde = new SpecificAvroSerde<>();
        valueSpecificAvroSerde.configure(serdeConfig, false);
        KStream<String, User> inputStream = kStreamBuilder
                                            .stream("test_topic",
                                                    Consumed.with(Serdes.String(),valueSpecificAvroSerde));

        KStream<String, User> outputStream = inputStream.filter((k,v) -> v.getName().contains("pass"));
        outputStream.to("out_topic");

        Topology build = kStreamBuilder.build();

        KafkaStreams kafkaStreams = new KafkaStreams(build,kStreamsConfig().asProperties());
        kafkaStreams.start();

        return outputStream;
    }
}
