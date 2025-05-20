package com.atc.atcconverter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LagosGeocoder {
    private static final String MAPBOX_API_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places/";
    private static final String ACCESS_TOKEN = "pk.eyJ1Ijoiby1jaHVrd3VrYXNpIiwiYSI6ImNtYXYwcTE2dTAwMXMya3M4NXo3Z2k1cHMifQ.cXZ0zN8loENSXW1-lj8-4g"; // Replace with your public token

    private static final String LAGOS_PROXIMITY = "3.3792,6.5244";
    private static final String SEARCH_TYPES = "poi,address,neighborhood,region";

    private static final Map<String, String> TRANSPORT_POINTS = new HashMap<>();
    static {
        TRANSPORT_POINTS.put("Ojota Bus Stop", "Ojota");
        TRANSPORT_POINTS.put("CMS Bus Stop", "Victoria Island");
        TRANSPORT_POINTS.put("Oshodi Underbridge", "Oshodi");
        TRANSPORT_POINTS.put("Palmgroove Bus Stop", "Palmgroove");
        TRANSPORT_POINTS.put("Iyana Ipaja Roundabout", "Iyana Ipaja");
        TRANSPORT_POINTS.put("Anthony Junction", "Anthony");
        TRANSPORT_POINTS.put("Cele Bus Stop", "Okota");
        TRANSPORT_POINTS.put("Seven-Up Bus Stop", "Oregun");
        TRANSPORT_POINTS.put("Leventis Bus Stop", "Marina");
        TRANSPORT_POINTS.put("Costain Roundabout", "Costain");
    }

    public static void main(String[] args) {
        for (Map.Entry<String, String> entry : TRANSPORT_POINTS.entrySet()) {
            String location = entry.getKey();
            String area = entry.getValue();

            try {
                double[] coordinates = getTransportPointCoordinates(location, area);
                System.out.printf("%-25s (%-20s) → Lat: %.6f, Lng: %.6f%n",
                        location, area, coordinates[0], coordinates[1]);
                Thread.sleep(200);
            } catch (Exception e) {
                System.err.println("Error processing " + location + ": " + e.getMessage());
            }
        }
    }

    public static double[] getTransportPointCoordinates(String location, String area) throws IOException {
        String[] queryAttempts = {
                String.format("%s, %s, Lagos", location, area),
                String.format("%s, %s", location, area),
                location,
                area
        };

        for (String query : queryAttempts) {
            try {
                double[] coords = attemptGeocode(query);
                if (coords != null) {
                    return coords;
                }
            } catch (IOException e) {
            }
        }

        throw new IOException("All geocoding attempts failed for: " + location);
    }

    private static double[] attemptGeocode(String query) throws IOException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String requestUrl = String.format("%s%s.json?country=ng&types=%s&proximity=%s&access_token=%s",
                MAPBOX_API_URL, encodedQuery, SEARCH_TYPES, LAGOS_PROXIMITY, ACCESS_TOKEN);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(requestUrl);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonNode rootNode = new ObjectMapper().readTree(jsonResponse);

                if (rootNode.has("message")) {
                    throw new IOException("Mapbox API error: " + rootNode.get("message").asText());
                }

                JsonNode features = rootNode.get("features");
                if (features != null && features.size() > 0) {
                    JsonNode firstResult = features.get(0);
                    JsonNode center = firstResult.get("center");
                    if (center != null && center.size() >= 2) {
                        return new double[]{center.get(1).asDouble(), center.get(0).asDouble()};
                    }
                }
            }
        }
        return null;
    }
}