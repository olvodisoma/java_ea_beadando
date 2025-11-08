package hu.nje.java_ea_beadando.dto;

import java.util.List;

public record HistoryResponse(
        String instrument,
        String granularity,
        List<CandleDto> candles
) {}