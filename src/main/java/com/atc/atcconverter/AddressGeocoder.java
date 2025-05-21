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
import java.util.Scanner;

public class AddressGeocoder {
    private static final String MAPBOX_API_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places/";
    private static final String ACCESS_TOKEN = "pk.eyJ1Ijoiby1jaHVrd3VrYXNpIiwiYSI6ImNtYXYwcTE2dTAwMXMya3M4NXo3Z2k1cHMifQ.cXZ0zN8loENSXW1-lj8-4g";
    private static final String COUNTRY = "ng";
    private static final String PROXIMITY = "3.3792,6.5244"; // Lagos coordinates
    private static final String[] SEARCH_TYPES = {"poi", "address", "neighborhood", "place"};

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enhanced Lagos Location Finder");
        System.out.println("------------------------------");
        System.out.println("Enter a location in Lagos (or 'quit' to exit):");
        System.out.println("Examples:");
        System.out.println("- Yaba Shopping Complex");
        System.out.println("- Computer Village, Ikeja");
        System.out.println("- Ojota Bus Stop");

        while (true) {
            System.out.print("\nLocation: ");
            String location = scanner.nextLine().trim();

            if (location.equalsIgnoreCase("quit")) {
                break;
            }

            try {
                GeocodeResult result = findLocation(location);
                System.out.println("\nBest Match Found:");
                System.out.println("-----------------");
                System.out.println("Name: " + result.placeName);
                System.out.printf("Coordinates: Latitude %.6f, Longitude %.6f%n",
                        result.latitude, result.longitude);
                System.out.println("Relevance: " + (result.relevance * 100) + "%");
                System.out.println("Type: " + result.featureType);
                System.out.println("\nVerify on Map:");
                System.out.println("https://www.google.com/maps?q=" +
                        result.latitude + "," + result.longitude);

                if (result.relevance < 0.7) {
                    System.out.println("\nWarning: This match might not be exact.");
                    System.out.println("Try adding more details like:");
                    System.out.println("- Nearby landmarks");
                    System.out.println("- Street names");
                    System.out.println("- Area/district");
                }
            } catch (LocationNotFoundException e) {
                System.err.println("\nError: " + e.getMessage());
                System.err.println("\nSuggestions to improve your search:");
                System.err.println("1. Try alternative names (e.g., 'Yaba Market' instead of 'Yaba Shopping Complex')");
                System.err.println("2. Add the area (e.g., 'Yaba Shopping Complex, Yaba')");
                System.err.println("3. Include nearby landmarks (e.g., 'near Yaba Tech')");
                System.err.println("4. Check for typos in the name");

                // Try a fallback search with simpler query
                try {
                    System.out.println("\nAttempting fallback search...");
                    GeocodeResult fallbackResult = findLocation(location.split(",")[0].trim());
                    System.out.println("\nFallback result found:");
                    System.out.printf("Coordinates: %.6f, %.6f%n",
                            fallbackResult.latitude, fallbackResult.longitude);
                    System.out.println("Name: " + fallbackResult.placeName);
                } catch (LocationNotFoundException ex) {
                    System.err.println("Could not find any matching locations.");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (IOException e) {
                System.err.println("API Error: " + e.getMessage());
            }
        }

        scanner.close();
        System.out.println("Geocoding service ended");
    }

    public static GeocodeResult findLocation(String location) throws IOException, LocationNotFoundException {
        String encodedLocation = URLEncoder.encode(location + ", Lagos", StandardCharsets.UTF_8);

        // First try with all search types
        String requestUrl = String.format(
                "%s%s.json?country=%s&proximity=%s&access_token=%s&types=%s",
                MAPBOX_API_URL, encodedLocation, COUNTRY, PROXIMITY, ACCESS_TOKEN,
                String.join(",", SEARCH_TYPES)
        );

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(requestUrl);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                return parseBestResult(jsonResponse, location);
            }
        }
    }

    private static GeocodeResult parseBestResult(String jsonResponse, String originalQuery)
            throws IOException, LocationNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonResponse);

        if (rootNode.has("message")) {
            throw new IOException(rootNode.get("message").asText());
        }

        JsonNode features = rootNode.get("features");
        if (features == null || features.isEmpty()) {
            throw new LocationNotFoundException("No results found for: " + originalQuery);
        }

        // Find the result with highest relevance
        JsonNode bestFeature = null;
        double highestRelevance = 0;
        for (JsonNode feature : features) {
            double relevance = feature.get("relevance").asDouble();
            if (relevance > highestRelevance) {
                highestRelevance = relevance;
                bestFeature = feature;
            }
        }

        if (bestFeature == null || highestRelevance < 0.3) {
            throw new LocationNotFoundException("No good matches found for: " + originalQuery);
        }

        JsonNode center = bestFeature.get("center");
        String featureType = bestFeature.get("place_type").get(0).asText();
        return new GeocodeResult(
                center.get(1).asDouble(), // latitude
                center.get(0).asDouble(), // longitude
                bestFeature.get("place_name").asText(),
                highestRelevance,
                featureType
        );
    }

    static class GeocodeResult {
        final double latitude;
        final double longitude;
        final String placeName;
        final double relevance;
        final String featureType;

        public GeocodeResult(double latitude, double longitude,
                             String placeName, double relevance,
                             String featureType) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.placeName = placeName;
            this.relevance = relevance;
            this.featureType = featureType;
        }
    }

    static class LocationNotFoundException extends Exception {
        public LocationNotFoundException(String message) {
            super(message);
        }
    }
}