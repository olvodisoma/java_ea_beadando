package hu.nje.java_ea_beadando.trade;

public record OpenOrderRequest(
        String instrument,  // pl. "EUR_USD"
        long units          // >0 Long, <0 Short, !=0
) {}