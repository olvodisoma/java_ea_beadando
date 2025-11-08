package hu.nje.java_ea_beadando.dto;

import java.time.Instant;

public record CandleDto(
        Instant time,
        double open,
        double high,
        double low,
        double close
) {}