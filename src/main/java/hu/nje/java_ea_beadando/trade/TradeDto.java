package hu.nje.java_ea_beadando.trade;

public record TradeDto(
        String tradeId,
        String instrument,
        long   units,       
        double openPrice,
        String openTime,
        String state,        
        Double unrealizedPL    
) {}
