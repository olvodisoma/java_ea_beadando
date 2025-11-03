package hu.nje.java_ea_beadando.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Controller
public class ForexAccountController {

    @GetMapping("/forex/account")
    public String getForexAccount(Model model) {
        try {
            // 🔹 Ingyenes, kulcs nélküli REST API
            String url = "https://open.er-api.com/v6/latest/USD";

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            System.out.println("=== REST API RESPONSE ===");
            System.out.println(response);

            if (response == null || !response.containsKey("rates")) {
                model.addAttribute("error", "Nem sikerült lekérni az adatokat a REST API-ból.");
                return "forex_account";
            }

            model.addAttribute("base", response.get("base_code"));
            model.addAttribute("date", response.get("time_last_update_utc"));
            model.addAttribute("rates", response.get("rates"));

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Hiba történt a REST hívás során: " + e.getMessage());
        }

        return "forex_account";
    }
}
