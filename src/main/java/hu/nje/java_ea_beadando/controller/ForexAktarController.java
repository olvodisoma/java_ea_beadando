package hu.nje.java_ea_beadando.controller;

import hu.nje.java_ea_beadando.service.OandaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/forex/aktar")
public class ForexAktarController {

    @Autowired
    private OandaService oandaService;

    // Devizapárok listája (ez jelenik meg a legördülőben)
    private final List<String> pairs = List.of("EUR_USD", "GBP_USD", "USD_JPY", "EUR_HUF", "USD_HUF");

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("pairs", pairs);
        return "forex_aktar";
    }

    @PostMapping
    public String getPrice(@RequestParam("pair") String pair, Model model) {
        model.addAttribute("pairs", pairs);
        model.addAttribute("selectedPair", pair);

        try {
            Map<String, Object> response = oandaService.getPricing(pair);

            // az OANDA válasz JSON szerkezete: {"prices":[{"instrument":"EUR_USD","bids":[{"price":"1.08234"}], ...}]}
            var prices = (List<Map<String, Object>>) response.get("prices");
            if (prices != null && !prices.isEmpty()) {
                Map<String, Object> priceData = prices.get(0);
                List<Map<String, String>> bids = (List<Map<String, String>>) priceData.get("bids");
                if (bids != null && !bids.isEmpty()) {
                    String bid = bids.get(0).get("price");
                    String base = pair.split("_")[0];
                    String target = pair.split("_")[1];
                    model.addAttribute("rate", bid);
                    model.addAttribute("base", base);
                    model.addAttribute("target", target);
                }
            }
        } catch (Exception e) {
            model.addAttribute("error", "Nem sikerült lekérni az árfolyamot: " + e.getMessage());
        }

        return "forex_aktar";
    }
}
