package com.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;

@Configuration
public class WordCountProcessor {

    @Bean
    public KStream<String, String> kStream(StreamsBuilder builder) {
        // 1. Define the input stream with explicit Serdes
        KStream<String, String> textLines = builder.stream(
                "text-input",
                Consumed.with(Serdes.String(), Serdes.String())
        );

        // 2. Word count processing with explicit type parameters
        KTable<String, Long> wordCounts = textLines
                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                .groupBy(
                        (key, word) -> word,
                        Grouped.<String, String>as("word-grouped")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(Serdes.String())
                )
                .count(
                        Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("word-counts-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(Serdes.Long())
                );

        // 3. Output to topic with explicit Serdes
        wordCounts.toStream()
                .to(
                        "word-count-output",
                        Produced.with(Serdes.String(), Serdes.Long())
                );

        return textLines;
    }
}