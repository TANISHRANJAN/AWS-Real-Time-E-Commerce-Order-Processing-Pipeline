package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class OrderProcessorHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        // Determine if event is a list of records or a single record
        List<?> records;
        if (event.get("Records") instanceof List) {
            // typical DynamoDB stream format: { "Records": [ … ] }
            records = (List<?>) event.get("Records");
        } else if (event instanceof List) {
            // sometimes direct list
            records = (List<?>) event;
        } else {
            // wrap single event
            records = List.of(event);
        }

        for (Object obj : records) {
            if (!(obj instanceof Map)) {
                context.getLogger().log("Skipping record because not a Map: " + obj);
                continue;
            }
            Map<String, Object> record = (Map<String, Object>) obj;

            Map<String, Object> dynamodb = safeCastMap(record.get("dynamodb"));
            if (dynamodb == null) {
                context.getLogger().log("Skipping record with no dynamodb section: " + record);
                continue;
            }
            Map<String, Object> newImage = safeCastMap(dynamodb.get("NewImage"));
            if (newImage == null) {
                context.getLogger().log("Skipping record with no NewImage: " + record);
                continue;
            }

            // extract fields
            String orderId = extractString(newImage, "orderId");
            String status = extractString(newImage, "status");
            String amountStr = extractNumberAsString(newImage, "amount");
            String email = extractString(newImage, "customerEmail");

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (Exception e) {
                context.getLogger().log(
                    String.format("Skipping order %s: invalid amount %s", orderId, amountStr)
                );
                continue;
            }

            if (amount <= 100) {
                context.getLogger().log(
                    String.format("Skipping order %s: amount %.2f <= 100", orderId, amount)
                );
                continue;
            }

            // Process the order
            context.getLogger().log(
                String.format("Processing order %s | Status=%s | Amount=%.2f | Email=%s",
                              orderId, status, amount, email)
            );

            // Add your business logic here (e.g., notify, update DB, etc.)
        }

        // Return status – you may customise the output as needed
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        return response;
    }

    /** Safely cast an object to a Map<String,Object> if possible */
    @SuppressWarnings("unchecked")
    private Map<String, Object> safeCastMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return null;
    }

    /** Extracts a String from DynamoDB attribute map for keyName where the attribute is of the form { "S": "value" } */
    private String extractString(Map<String, Object> imageMap, String keyName) {
        Object attrObj = imageMap.get(keyName);
        if (attrObj instanceof Map) {
            Map<String,Object> attr = (Map<String,Object>) attrObj;
            Object s = attr.get("S");
            if (s instanceof String) {
                return (String) s;
            }
        }
        return null;
    }

    /** Extracts the numeric value (as string) from DynamoDB attribute map for keyName where it is of the form { "N": "123.45" } */
    private String extractNumberAsString(Map<String, Object> imageMap, String keyName) {
        Object attrObj = imageMap.get(keyName);
        if (attrObj instanceof Map) {
            Map<String,Object> attr = (Map<String,Object>) attrObj;
            Object n = attr.get("N");
            if (n instanceof String) {
                return (String) n;
            }
        }
        return null;
    }
}
