package hu.nje.java_ea_beadando.forex;

import hu.nje.java_ea_beadando.dto.HistoryResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/forex")
public class ForexController {

    private final ForexService service;

    public ForexController(ForexService service) {
        this.service = service;
    }

    @GetMapping("/ping")
    public Map<String,String> ping() {
        return Map.of("status","ok");
    }

    @GetMapping("/history")
    public HistoryResponse history(@RequestParam String instrument,
                                   @RequestParam(defaultValue = "D") String granularity) {
        return service.lastHistory(instrument, granularity);
    }
}
