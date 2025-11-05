package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class PremiumOrderProcessor implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        List<Map<String,Object>> records;

        // Determine if the event is a list of records or single record
        if (event.get("Records") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recs = (List<Map<String, Object>>) event.get("Records");
            records = recs;
        } else if (event instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String,Object>> recs = (List<Map<String,Object>>) event;
            records = recs;
        } else {
            // wrap single event
            @SuppressWarnings("unchecked")
            Map<String,Object> single = event;
            records = new ArrayList<>();
            records.add(single);
        }

        List<String> premiumOrders = new ArrayList<>();

        for (Map<String, Object> record : records) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dynamodb = (Map<String,Object>) record.getOrDefault("dynamodb", Map.of());
            @SuppressWarnings("unchecked")
            Map<String, Object> newImage   = (Map<String,Object>) dynamodb.getOrDefault("NewImage", Map.of());
            @SuppressWarnings("unchecked")
            Map<String, Object> oldImage   = (Map<String,Object>) dynamodb.getOrDefault("OldImage", Map.of());

            String orderId        = extractString(newImage, "orderId");
            String newStatus      = extractString(newImage, "status");
            String oldStatus      = extractString(oldImage, "status");
            String amountStr      = extractNumberAsString(newImage, "amount", "0");
            String customerEmail  = extractString(newImage, "customerEmail", "");

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (Exception e) {
                amount = 0;
            }

            if ("pending".equalsIgnoreCase(oldStatus)
                && "shipped".equalsIgnoreCase(newStatus)
                && amount > 1000
                && (customerEmail == null || !customerEmail.toLowerCase().contains("test.com"))) {

                context.getLogger().log(String.format(
                    "🚀 Premium order shipped! Order ID: %s | Amount=%.2f", orderId, amount));
                premiumOrders.add(orderId);

            } else {
                context.getLogger().log(String.format(
                    "Skipping order %s | Amount=%.2f, Status %s->%s, Email=%s",
                    orderId, amount, oldStatus, newStatus, customerEmail));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        response.put("premiumOrdersProcessed", premiumOrders);
        return response;
    }

    private String extractString(Map<String, Object> imageMap, String keyName) {
        if (imageMap == null) return null;
        Object attrObj = imageMap.get(keyName);
        if (attrObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attr = (Map<String, Object>) attrObj;
            Object s = attr.get("S");
            if (s instanceof String) {
                return (String)s;
            }
        }
        return null;
    }

    private String extractNumberAsString(Map<String, Object> imageMap, String keyName, String defaultValue) {
        if (imageMap == null) return defaultValue;
        Object attrObj = imageMap.get(keyName);
        if (attrObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attr = (Map<String, Object>) attrObj;
            Object n = attr.get("N");
            if (n instanceof String) {
                return (String) n;
            }
        }
        return defaultValue;
    }
}
