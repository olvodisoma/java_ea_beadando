package hu.nje.java_ea_beadando.trade;

import hu.nje.java_ea_beadando.forex.OandaClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class TradeService {
    private final OandaClient oanda;

    // egyszerű whitelist (bővíthető)
    private static final Set<String> ALLOWED = Set.of(
            "EUR_USD","GBP_USD","USD_JPY","EUR_HUF","USD_HUF"
    );

    public TradeService(OandaClient oanda) {
        this.oanda = oanda;
    }

    public OpenOrderResult open(OpenOrderRequest req) {
        if (req == null || req.instrument() == null || req.instrument().isBlank())
            return new OpenOrderResult("error", null, null, 0, null, null, "Instrument megadása kötelező.");
        if (req.units() == 0)
            return new OpenOrderResult("error", null, req.instrument(), 0, null, null, "Units nem lehet 0.");
        if (!ALLOWED.contains(req.instrument()))
            return new OpenOrderResult("error", null, req.instrument(), req.units(), null, null, "Nem engedélyezett instrumentum.");

        return oanda.openMarketOrder(req.instrument(), req.units());
    }

        public List<TradeDto> openPositions() {
        return oanda.listOpenTrades();
    }

}
