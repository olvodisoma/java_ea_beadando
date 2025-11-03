package hu.nje.java_ea_beadando.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Controller
public class ForexAktarController {

    @GetMapping("/forex/aktar")
    public String showForm(Model model) {
        // Elérhető devizapárok a lenyíló listához
        String[] pairs = {"EUR/USD", "GBP/USD", "HUF/USD", "JPY/USD", "CHF/USD"};
        model.addAttribute("pairs", pairs);
        return "forex_aktar";
    }

    @PostMapping("/forex/aktar")
    public String getCurrentRate(@RequestParam("pair") String pair, Model model) {
        String[] parts = pair.split("/");
        String base = parts[0];
        String target = parts[1];

        try {
            String url = "https://open.er-api.com/v6/latest/" + base;
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("rates")) {
                model.addAttribute("error", "Nem sikerült lekérni az adatokat a REST API-ból.");
                return "forex_aktar";
            }

            Map<String, Object> rates = (Map<String, Object>) response.get("rates");
            Object rateValue = rates.get(target);

            model.addAttribute("selectedPair", pair);
            model.addAttribute("base", base);
            model.addAttribute("target", target);
            model.addAttribute("rate", rateValue);
            model.addAttribute("pairs", new String[]{"EUR/USD", "GBP/USD", "HUF/USD", "JPY/USD", "CHF/USD"});

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Hiba történt a REST hívás során: " + e.getMessage());
        }

        return "forex_aktar";
    }
}
