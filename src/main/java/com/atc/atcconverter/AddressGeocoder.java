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
import java.util.*;

public class AddressGeocoder {
    private static final String MAPBOX_API_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places/";
    private static final String ACCESS_TOKEN = "pk.eyJ1Ijoiby1jaHVrd3VrYXNpIiwiYSI6ImNtYXYwcTE2dTAwMXMya3M4NXo3Z2k1cHMifQ.cXZ0zN8loENSXW1-lj8-4g";
    private static final double MIN_RELEVANCE = 0.65
    ;

    private static final Map<String, double[]> LAGOS_LANDMARKS = Map.of(
            "yaba ultra modern market", new double[]{6.5193, 3.3791},
            "yaba shopping complex", new double[]{6.5193, 3.3791},
            "computer village ikeja", new double[]{6.5928, 3.3427},
            "ojota bus stop", new double[]{6.5789, 3.3810},
            "oshodi transport interchange", new double[]{6.5560, 3.3299}
    );

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Lagos Precision Location Finder");
        System.out.println("---------------------------------");
        System.out.println("Known locations include:");
        System.out.println("- Yaba Ultra Modern Market");
        System.out.println("- Computer Village Ikeja");
        System.out.println("- Ojota Bus Stop");
        System.out.println("- Oshodi Transport Interchange");

        while (true) {
            System.out.print("\nEnter location (or 'quit'): ");
            String query = scanner.nextLine().trim().toLowerCase();

            if (query.equals("quit")) break;

            try {
                LocationResult result = findExactLocation(query);
                System.out.println("\nLocation Found:");
                System.out.println("---------------");
                System.out.println("Name: " + result.name);
                System.out.printf("Coordinates: %.6f, %.6f%n", result.latitude, result.longitude);
                System.out.println("Source: " + result.source);
                System.out.println("\nVerify on Google Maps:");
                System.out.println("https://www.google.com/maps?q=" + result.latitude + "," + result.longitude);

                if (result.source.equals("Mapbox")) {
                    System.out.println("\nNote: For better precision, try adding:");
                    System.out.println("- Nearby street (e.g., 'Herbert Macaulay Way')");
                    System.out.println("- Specific area (e.g., 'in Yaba')");
                }
            } catch (LocationNotFoundException | IOException e) {
                System.out.println("\nError: " + e.getMessage());
                System.out.println("\nDid you mean:");
                LAGOS_LANDMARKS.keySet().stream()
                        .filter(name -> name.contains(query.split(" ")[0]))
                        .forEach(name -> System.out.println("- " + name));
            }
        }
    }

    public static LocationResult findExactLocation(String query) throws LocationNotFoundException, IOException {
        if (LAGOS_LANDMARKS.containsKey(query)) {
            double[] coords = LAGOS_LANDMARKS.get(query);
            return new LocationResult(query, coords[0], coords[1], "Local Database");
        }

        LocationResult result = tryMapboxSearch(query);
        if (result != null) return result;

        // Try common variations
        for (String variation : getCommonVariations(query)) {
            result = tryMapboxSearch(variation);
            if (result != null) return result;
        }

        throw new LocationNotFoundException("Could not find: " + query);
    }

    private static LocationResult tryMapboxSearch(String query) throws IOException {
        String encoded = URLEncoder.encode(query + ", Lagos", StandardCharsets.UTF_8);
        String url = MAPBOX_API_URL + encoded + ".json?country=ng&access_token=" + ACCESS_TOKEN + "&limit=1";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = client.execute(request)) {
                String json = EntityUtils.toString(response.getEntity());
                return parseMapboxResult(json, query);
            }
        }
    }

    private static LocationResult parseMapboxResult(String json, String query) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        if (root.has("features") && root.get("features").size() > 0) {
            JsonNode feature = root.get("features").get(0);
            double relevance = feature.get("relevance").asDouble();

            if (relevance >= MIN_RELEVANCE) {
                return new LocationResult(
                        feature.get("place_name").asText(),
                        feature.get("center").get(1).asDouble(),
                        feature.get("center").get(0).asDouble(),
                        "Mapbox (" + (int)(relevance*100) + "% match)"
                );
            }
        }
        return null;
    }

    private static List<String> getCommonVariations(String query) {
        List<String> variations = new ArrayList<>();

        if (query.contains("market")) {
            variations.add(query.replace("market", "shopping complex"));
        }
        if (query.contains("bus stop")) {
            variations.add(query.replace("bus stop", "garage"));
            variations.add(query.replace("bus stop", "motor park"));
        }
        if (!query.contains("lagos")) {
            variations.add(query + " lagos");
        }

        return variations;
    }

    static class LocationResult {
        final String name;
        final double latitude;
        final double longitude;
        final String source;

        public LocationResult(String name, double latitude, double longitude, String source) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.source = source;
        }
    }

    static class LocationNotFoundException extends Exception {
        public LocationNotFoundException(String message) {
            super(message);
        }
    }
}