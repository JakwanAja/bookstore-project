package backend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;             
import java.nio.file.Paths;

public class BookStoreServer{

    public static void main(String[] args) throws IOException{
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/calculate", new CalculateHandler());
        server.setExecutor(null);
        server.start();
    }

    static class CalculateHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes());

                String productId = extractJsonString(body, "product_id");
                int quantity = extractJsonInt(body, "quantity");

                double basePrice = getBasePrice(productId);

                double total = calculateTotal(quantity, basePrice);

                String response = "{\"total\": " + total + "}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());

                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private static String extractJsonString(String json, String key){
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start != -1) {
            start += search.length();
            int end = json.indexOf("\"", start);
            if (end > -1) {
                return json.substring(start, end);
            }
        }
        return "";
    }

    private static int extractJsonInt(String json, String key){
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start != -1) {
            start += search.length();
            int end = json.indexOf(",", start);
            if (end == -1) {
                end = json.indexOf("}", start);
            }
            if (end > -1) {
                String val = json.substring(start, end).trim();
                return Integer.parseInt(val);
            }
        }
        return 0;
    }

    private static double getBasePrice(String productId){
        try {
            String json = new String(Files.readAllBytes(Paths.get("../data/products.json")));
            String searchId = "\"id\":\"" + productId + "\"";
            String searchIdSpaced = "\"id\": \"" + productId + "\"";

            int idIdx = json.indexOf(searchId);
            if (idIdx == -1) {
                idIdx = json.indexOf(searchIdSpaced);
            }

            if (idIdx != -1) {
                int priceStart = json.indexOf("\"price\":", idIdx) + 8;
                int priceEnd = json.indexOf(",", priceStart);
                return Double.parseDouble(json.substring(priceStart, priceEnd).trim());
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public static double calculateTotal(int quantity, double basePrice){
        double discount = 0.0;
        double totalPrice = quantity * basePrice;

        if (quantity > 0) {
            if (quantity >= 100) {
                if (basePrice > 100000) {
                    discount = 0.30;
                } else {
                    discount = 0.25;
                }
            } else if (quantity >= 50) {
                if (basePrice > 100000) {
                    discount = 0.20;
                } else {
                    discount = 0.15;
                }
            } else if (quantity >= 10) {
                discount = 0.10;
            } else {
                discount = 0.0;
            }
        } else {
            return 0.0;
        }

        return totalPrice - (totalPrice * discount);
    }
}