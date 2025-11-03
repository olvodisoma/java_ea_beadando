package hu.nje.java_ea_beadando.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class SoapController {

    // --- HTTP végpont az SSL hibák elkerülésére ---
    private static final String MNB_URL = "http://www.mnb.hu/arfolyamok.asmx";

    @GetMapping("/soap")
    public String soapPage() {
        return "soap";
    }

    @PostMapping("/soap")
    public String getExchangeRates(
            @RequestParam("currency") String currency,
            @RequestParam("start") String start,
            @RequestParam("end") String end,
            Model model) {

        try {
            // 1️⃣ SOAP kérés
            String xmlResponse = callMnbSoap(currency, start, end);

            // 2️⃣ XML feldolgozás
            LinkedHashMap<String, Double> rates = parseMnbXml(xmlResponse);

            if (rates.isEmpty()) {
                model.addAttribute("error", "Nem található árfolyam a megadott időszakra.");
            } else {
                // 3️⃣ JSON-serializáció (Thymeleaf-kompatibilis)
                List<String> labels = new ArrayList<>(rates.keySet());
                List<Double> values = new ArrayList<>(rates.values());

                ObjectMapper mapper = new ObjectMapper();
                model.addAttribute("currency", currency);
                model.addAttribute("rates", rates);
                model.addAttribute("labelsJson", mapper.writeValueAsString(labels));
                model.addAttribute("valuesJson", mapper.writeValueAsString(values));
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Hiba történt az adatok lekérésekor: " + e.toString());
        }

        return "soap";
    }

    // ===== MNB SOAP hívás =====
    private String callMnbSoap(String currency, String start, String end) throws Exception {
        String body =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                        + "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                        + "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                        + "<soap:Body>"
                        + "<GetExchangeRates xmlns=\"http://www.mnb.hu/webservices/\">"
                        + "<startDate>" + start + "</startDate>"
                        + "<endDate>" + end + "</endDate>"
                        + "<currencyNames>" + currency + "</currencyNames>"
                        + "</GetExchangeRates>"
                        + "</soap:Body>"
                        + "</soap:Envelope>";

        URL url = new URL(MNB_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int code = connection.getResponseCode();
        InputStream is = (code >= 200 && code < 400)
                ? connection.getInputStream()
                : connection.getErrorStream();

        if (is == null) {
            throw new IOException("Nem sikerült adatfolyamot megnyitni. HTTP kód: " + code);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) response.append(line);
        }

        // Debug: első 500 karakter az XML-ből
        System.out.println("SOAP RESPONSE (first 500 chars):\n" + response.substring(0, Math.min(500, response.length())));

        return response.toString();
    }

    // ===== XML feldolgozás =====
    private LinkedHashMap<String, Double> parseMnbXml(String xml) throws Exception {
        LinkedHashMap<String, Double> map = new LinkedHashMap<>();

        if (xml == null || xml.isEmpty()) {
            throw new RuntimeException("Üres XML válasz érkezett az MNB-től.");
        }

        // 1️⃣ Első XML feldolgozás – SOAP Envelope
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document outerDoc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        // 2️⃣ A <GetExchangeRatesResult> mező szövegének kinyerése
        NodeList resultNodes = outerDoc.getElementsByTagNameNS("http://www.mnb.hu/webservices/", "GetExchangeRatesResult");
        if (resultNodes.getLength() == 0) {
            throw new RuntimeException("Nem található GetExchangeRatesResult elem az XML-ben.");
        }
        String innerXml = resultNodes.item(0).getTextContent();

        // 3️⃣ A belső XML újraparse-olása
        Document innerDoc = builder.parse(new ByteArrayInputStream(innerXml.getBytes(StandardCharsets.UTF_8)));

        // 4️⃣ A <Rate> elemek kinyerése a belső XML-ből
        NodeList rateNodes = innerDoc.getElementsByTagName("Rate");
        for (int i = 0; i < rateNodes.getLength(); i++) {
            Element rateElem = (Element) rateNodes.item(i);
            Element dayElem = (Element) rateElem.getParentNode();

            String date = dayElem.getAttribute("date");
            String rateText = rateElem.getTextContent().trim().replace(",", ".");

            if (!date.isEmpty() && !rateText.isEmpty()) {
                try {
                    double value = Double.parseDouble(rateText);
                    map.put(date, value);
                } catch (NumberFormatException ignored) {}
            }
        }

        return map;
    }
}

