package hu.nje.java_ea_beadando.forex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.nje.java_ea_beadando.dto.CandleDto;
import hu.nje.java_ea_beadando.dto.HistoryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;



@Component
public class OandaClient {

    private final RestTemplate rest;
    private final ObjectMapper om = new ObjectMapper();
    private final String baseUrl;
    private final String token;

    // 🔸 Itt igazítjuk a @Value kulcsokat a te application.properties fájlodhoz
    public OandaClient(
            RestTemplate restTemplate,
            @Value("${oanda.api.url}") String baseUrl,
            @Value("${oanda.api.key}") String token
    ) {
        this.rest = restTemplate;
        this.baseUrl = baseUrl;
        this.token = token;
    }

    public HistoryResponse lastCandles(String instrument, String granularity, int count) {
        try {
            String url = String.format(
                    "%s/instruments/%s/candles?granularity=%s&count=%d&price=M",
                    baseUrl, instrument, granularity, count
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> req = new HttpEntity<>(headers);

            ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "OANDA hívás sikertelen");
            }

            JsonNode root = om.readTree(resp.getBody());
            JsonNode candlesNode = root.get("candles");
            if (candlesNode == null || !candlesNode.isArray()) {
                throw new ResponseStatusException(BAD_GATEWAY, "Váratlan OANDA válasz (nincs 'candles').");
            }

            List<CandleDto> candles = new ArrayList<>();
            for (JsonNode c : candlesNode) {
                if (!c.path("complete").asBoolean(false)) continue;
                String time = c.path("time").asText();
                JsonNode mid = c.path("mid");
                double o = mid.path("o").asDouble();
                double h = mid.path("h").asDouble();
                double l = mid.path("l").asDouble();
                double cl = mid.path("c").asDouble();
                candles.add(new CandleDto(Instant.parse(time), o, h, l, cl));
            }

            candles = candles.stream()
                    .sorted(Comparator.comparing(CandleDto::time))
                    .collect(Collectors.toList());
            if (candles.size() > count) {
                candles = candles.subList(candles.size() - count, candles.size());
            }

            return new HistoryResponse(instrument, granularity, candles);

        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY,
                    "Nem sikerült lekérni a historikus árakat: " + ex.getMessage(), ex);
        }
    }

@Value("${oanda.account.id}")
private String accountId;
public hu.nje.java_ea_beadando.trade.OpenOrderResult openMarketOrder(String instrument, long units) {
    try {
        String url = String.format("%s/accounts/%s/orders", baseUrl, accountId);

        // OANDA v20 Market Order request body
        String bodyJson = """
        {
          "order": {
            "instrument": "%s",
            "units": "%s",
            "type": "MARKET",
            "timeInForce": "FOK",
            "positionFill": "DEFAULT"
          }
        }
        """.formatted(instrument, Long.toString(units));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> req = new HttpEntity<>(bodyJson, headers);
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.POST, req, String.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            return new hu.nje.java_ea_beadando.trade.OpenOrderResult(
                    "rejected", null, instrument, units, null, null,
                    "OANDA válaszkód: " + resp.getStatusCode()
            );
        }

        JsonNode root = om.readTree(resp.getBody());

        // Sikeres azonnali teljesülés
        JsonNode fill = root.get("orderFillTransaction");
        if (fill != null) {
            String id   = fill.path("id").asText(null);
            String pr   = fill.path("price").asText(null);
            String time = fill.path("time").asText(null);
            long u      = fill.path("units").asLong(units);

            return new hu.nje.java_ea_beadando.trade.OpenOrderResult(
                    "filled", id, instrument, u, pr, time, "Piaci megbízás teljesült."
            );
        }

        // Létrejött, de nem töltődött ki azonnal
        JsonNode created = root.get("orderCreateTransaction");
        if (created != null) {
            String id   = created.path("id").asText(null);
            String time = created.path("time").asText(null);
            long u      = created.path("units").asLong(units);
            return new hu.nje.java_ea_beadando.trade.OpenOrderResult(
                    "created", id, instrument, u, null, time, "Piaci megbízás létrehozva."
            );
        }

        // Elutasítás
        JsonNode reject = root.get("orderRejectTransaction");
        if (reject != null) {
            String id   = reject.path("id").asText(null);
            String time = reject.path("time").asText(null);
            String msg  = reject.path("reason").asText("Order rejected");
            return new hu.nje.java_ea_beadando.trade.OpenOrderResult(
                    "rejected", id, instrument, units, null, time, msg
            );
        }

        // Váratlan forma
        return new hu.nje.java_ea_beadando.trade.OpenOrderResult(
                "unknown", null, instrument, units, null, null,
                "Váratlan OANDA válasz."
        );

    } catch (Exception ex) {
        return new hu.nje.java_ea_beadando.trade.OpenOrderResult(
                "error", null, instrument, units, null, null,
                "Hiba a megbízásnál: " + ex.getMessage()
        );
    }
}

public java.util.List<hu.nje.java_ea_beadando.trade.TradeDto> listOpenTrades() {
    try {
        String url = String.format("%s/accounts/%s/openTrades", baseUrl, accountId);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(java.util.List.of(org.springframework.http.MediaType.APPLICATION_JSON));

        org.springframework.http.HttpEntity<Void> req = new org.springframework.http.HttpEntity<>(headers);
        org.springframework.http.ResponseEntity<String> resp =
                rest.exchange(url, org.springframework.http.HttpMethod.GET, req, String.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY, "OANDA hívás sikertelen (openTrades)");
        }

        com.fasterxml.jackson.databind.JsonNode root = om.readTree(resp.getBody());
        com.fasterxml.jackson.databind.JsonNode arr = root.get("trades");
        java.util.List<hu.nje.java_ea_beadando.trade.TradeDto> out = new java.util.ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode t : arr) {
                String id   = t.path("id").asText();
                String instr= t.path("instrument").asText();
                long units  = 0L;
                try { units = Long.parseLong(t.path("currentUnits").asText("0")); } catch (Exception ignore) {}
                double price= t.path("price").asDouble();
                String time = t.path("openTime").asText(null);
                String state= t.path("state").asText(null);
                Double upl  = t.has("unrealizedPL") ? t.path("unrealizedPL").asDouble() : null;

                out.add(new hu.nje.java_ea_beadando.trade.TradeDto(id, instr, units, price, time, state, upl));
            }
        }
        return out;
    } catch (org.springframework.web.server.ResponseStatusException rse) {
        throw rse;
    } catch (Exception ex) {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "Nem sikerült a nyitott pozíciók lekérése: " + ex.getMessage(), ex);
    }
}






}
