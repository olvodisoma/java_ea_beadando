package hu.nje.java_ea_beadando.controller;

import hu.nje.java_ea_beadando.service.OandaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class ForexAccountController {

    @Autowired
    private OandaService oandaService;

    @GetMapping("/forex/account")
    public String getAccountInfo(Model model) {
        try {
            Map<String, Object> data = oandaService.getAccountDetails();
            model.addAttribute("account", data.get("account"));
        } catch (Exception e) {
            model.addAttribute("error", "Nem sikerült lekérni az OANDA fiók adatait: " + e.getMessage());
        }
        return "forex_account";
    }
}
