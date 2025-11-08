package hu.nje.java_ea_beadando.trade;

public record OpenOrderResult(
        String status,       // "filled" / "created" / "rejected"
        String orderId,
        String instrument,
        long units,
        String price,        // kitöltési ár ha van
        String time,         // OANDA időbélyeg
        String message       // hiba / kiegészítő infó
) {}