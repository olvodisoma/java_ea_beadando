package hu.nje.java_ea_beadando.trade;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forex/trade")
public class TradeController {

    private final TradeService service;

    public TradeController(TradeService service) {
        this.service = service;
    }

    @PostMapping(value = "/open", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public OpenOrderResult open(@RequestBody OpenOrderRequest req) {
        return service.open(req);
    }

    @GetMapping(value = "/open-positions", produces = MediaType.APPLICATION_JSON_VALUE)
    public java.util.List<TradeDto> openPositions() {
        return service.openPositions();
    }

    @PostMapping(
    value = "/close",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CloseResult close(@RequestBody CloseRequest req) {
        return service.closeById(req.tradeId());
    }

}