package hu.nje.java_ea_beadando.forex;

import hu.nje.java_ea_beadando.dto.HistoryResponse;
import org.springframework.stereotype.Service;

@Service
public class ForexService {
    private final OandaClient client;

    public ForexService(OandaClient client) {
        this.client = client;
    }

    public HistoryResponse lastHistory(String instrument, String granularity) {
        // utolsó 10 gyertya
        return client.lastCandles(instrument, granularity, 10);
    }
}
