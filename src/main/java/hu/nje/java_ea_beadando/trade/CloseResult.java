package hu.nje.java_ea_beadando.trade;

public record CloseResult(
        String status,     // "closed" / "rejected" / "error"
        String tradeId,    // bezárt trade valódi ID-je vagy a kért azonosító
        String instrument, // instrumentum
        String price,      // záróár (ha elérhető)
        String time,       // időbélyeg
        String message     // kiegészítő infó
) {}