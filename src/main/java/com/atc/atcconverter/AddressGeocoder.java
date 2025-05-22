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
    private static final String COUNTRY = "ng";
    private static final String PROXIMITY = "3.3792,6.5244";
    private static final String[] SEARCH_TYPES = {"address", "poi", "neighborhood", "place"};
    private static final Set<String> LAGOS_DISTRICTS = new HashSet<>(Arrays.asList(
            "ikeja", "victoria island", "lekki", "festac", "surulere",
            "apapa", "yaba", "ojota", "agege", "ajah", "marina", "oshodi"
    ));

    private static final double STRICT_RELEVANCE = 0.7;
    private static final double FALLBACK_RELEVANCE = 0.6;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Precision Lagos Location Finder");
        System.out.println("-------------------------------");
        System.out.println("Enter a location in Lagos (or 'quit' to exit):");
        System.out.println("Examples with district hints:");
        System.out.println("- Second Rainbow Bus Stop, Festac");
        System.out.println("- Computer Village, Ikeja");
        System.out.println("- Jakande Roundabout, Lekki");

        while (true) {
            System.out.print("\nSearch Location: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit")) {
                break;
            }

            try {
                GeocodeResult result = findLocationInLagos(input);
                printResult(result, true);

                if (result.relevance < STRICT_RELEVANCE) {
                    System.out.println("\n[Note] This is a approximate match. For better results:");
                    System.out.println("- Include district names like 'Festac' or 'Ikeja'");
                    System.out.println("- Add 'Bus Stop' or 'Junction' when applicable");
                }
            } catch (LocationNotFoundException e) {
                System.err.println("\n[Error] " + e.getMessage());
                handleFailedSearch(input, scanner);
            } catch (IOException e) {
                System.err.println("\n[API Error] " + e.getMessage());
            }
        }

        scanner.close();
        System.out.println("\nService terminated");
    }

    private static void printResult(GeocodeResult result, boolean showMap) {
        System.out.println("\nBest Match Found:");
        System.out.println("-----------------");
        System.out.println("Name: " + result.placeName);
        System.out.printf("Coordinates: %.6f, %.6f%n", result.latitude, result.longitude);
        System.out.printf("Confidence: %.0f%%%n", result.relevance * 100);
        System.out.println("Category: " + result.featureType.toUpperCase());

        if (showMap) {
            System.out.println("\nView on Map: https://www.google.com/maps?q=" +
                    result.latitude + "," + result.longitude);
        }
    }

    private static void handleFailedSearch(String query, Scanner scanner) {
        System.out.println("\nSearch Tips:");
        System.out.println("1. Try adding a district: '" + query + ", [district]'");
        System.out.println("2. Use full names: 'Bus Stop', 'Market', 'Roundabout'");
        System.out.println("3. Common districts: Festac, Ikeja, Lekki, Victoria Island");

        System.out.print("\nTry improved search? (y/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            try {
                GeocodeResult result = findLocationInLagos(query + ", Lagos");
                printResult(result, false);
                System.out.println("\nAdd district for better accuracy");
            } catch (Exception e) {
                System.err.println("Still no matches found");
            }
        }
    }

    public static GeocodeResult findLocationInLagos(String location)
            throws IOException, LocationNotFoundException {
        String processedQuery = processQuery(location);
        String encodedQuery = URLEncoder.encode(processedQuery, StandardCharsets.UTF_8);

        String apiUrl = String.format(
                "%s%s.json?country=%s&proximity=%s&access_token=%s&types=%s&limit=8&autocomplete=true",
                MAPBOX_API_URL, encodedQuery, COUNTRY, PROXIMITY, ACCESS_TOKEN,
                String.join(",", SEARCH_TYPES)
        );

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl);

            try (CloseableHttpResponse response = client.execute(request)) {
                String json = EntityUtils.toString(response.getEntity());
                return parseResults(json, location);
            }
        }
    }

    private static String processQuery(String raw) {
        String base = raw.toLowerCase().contains("lagos") ? raw : raw + ", Lagos Nigeria";

        Optional<String> district = LAGOS_DISTRICTS.stream()
                .filter(d -> base.toLowerCase().contains(d))
                .findFirst();

        return district.map(d -> {
            String clean = base.replaceAll("(?i)"+d, "").trim();
            return clean + ", " + capitalize(d);
        }).orElse(base);
    }

    private static String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    private static GeocodeResult parseResults(String json, String query)
            throws IOException, LocationNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        if (root.has("message")) {
            throw new IOException(root.get("message").asText());
        }

        List<JsonNode> candidates = new ArrayList<>();
        double bestScore = FALLBACK_RELEVANCE;
        JsonNode bestFeature = null;

        for (JsonNode feature : root.get("features")) {
            if (!isInLagos(feature)) continue;

            double relevance = feature.get("relevance").asDouble();
            double score = calculateScore(feature, relevance);

            if (score > bestScore) {
                bestScore = score;
                bestFeature = feature;
            }

            if (score >= STRICT_RELEVANCE) {
                candidates.add(feature);
            }
        }

        if (bestFeature == null) {
            throw new LocationNotFoundException("No matches for: " + query);
        }

        if (!candidates.isEmpty()) {
            bestFeature = candidates.stream()
                    .max(Comparator.comparingDouble(f -> calculateScore(f, f.get("relevance").asDouble())))
                    .orElse(bestFeature);
        }

        return createResult(bestFeature);
    }

    private static double calculateScore(JsonNode feature, double baseRelevance) {
        String type = feature.get("place_type").get(0).asText();
        String name = feature.get("place_name").asText().toLowerCase();

        double score = baseRelevance;

        // Scoring boosts
        if (type.equals("address")) score += 0.15;
        if (type.equals("poi")) score += 0.1;
        if (name.contains("bus stop") || name.contains("junction")) score += 0.2;
        if (LAGOS_DISTRICTS.stream().anyMatch(name::contains)) score += 0.1;

        // Penalties
        if (type.equals("region")) score -= 0.3;
        if (name.contains("nigeria") && !name.contains("lagos")) score -= 0.4;

        return Math.min(score, 1.0);
    }

    private static boolean isInLagos(JsonNode feature) {
        for (JsonNode context : feature.get("context")) {
            if (context.get("id").asText().startsWith("region.") &&
                    context.get("text").asText().equalsIgnoreCase("Lagos")) {
                return true;
            }
        }
        return false;
    }

    private static GeocodeResult createResult(JsonNode feature) {
        JsonNode center = feature.get("center");
        return new GeocodeResult(
                center.get(1).asDouble(),
                center.get(0).asDouble(),
                feature.get("place_name").asText(),
                calculateScore(feature, feature.get("relevance").asDouble()),
                feature.get("place_type").get(0).asText()
        );
    }

    static class GeocodeResult {
        final double latitude;
        final double longitude;
        final String placeName;
        final double relevance;
        final String featureType;

        GeocodeResult(double lat, double lon, String name, double rel, String type) {
            latitude = lat;
            longitude = lon;
            placeName = name;
            relevance = rel;
            featureType = type;
        }
    }

    static class LocationNotFoundException extends Exception {
        LocationNotFoundException(String message) {
            super(message);
        }
    }
}