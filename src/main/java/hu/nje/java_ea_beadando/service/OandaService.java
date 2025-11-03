package hu.nje.java_ea_beadando.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OandaService {

    @Value("${oanda.api.url}")
    private String apiUrl;

    @Value("${oanda.account.id}")
    private String accountId;

    @Value("${oanda.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private Map<String, Object> makeRequest(String endpoint) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(endpoint, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }

    public Map<String, Object> getAccountDetails() {
        String url = apiUrl + "/accounts/" + accountId;
        return makeRequest(url);
    }

    public Map<String, Object> getPricing(String instruments) {
        String url = apiUrl + "/accounts/" + accountId + "/pricing?instruments=" + instruments;
        return makeRequest(url);
    }
}
