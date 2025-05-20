//package com.atc.atcconverter;
//
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.util.EntityUtils;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//
//public class LagosGeocoder {
//    private static final String MAPBOX_API_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places/";
//    private static final String ACCESS_TOKEN = "pk.eyJ1Ijoiby1jaHVrd3VrYXNpIiwiYSI6ImNtYXYwcTE2dTAwMXMya3M4NXo3Z2k1cHMifQ.cXZ0zN8loENSXW1-lj8-4g";
//
//    // Lagos bounding box (min lng, min lat, max lng, max lat)
//    private static final double[] LAGOS_BOUNDS = {3.2, 6.3, 3.6, 6.8};
//
//    public static double[] getCoordinates(String location) throws IOException {
//        String query = location + ", Lagos, Nigeria";
//        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
//        String url = MAPBOX_API_URL + encodedQuery + ".json" +
//                "?country=ng" +
//                "&types=poi,address,place" +
//                "&limit=1" +
//                "&access_token=" + ACCESS_TOKEN;
//
//        System.out.println("API Request URL: " + url); // Added logging
//
//        try {
//            JsonNode response = executeRequest(url);
//            System.out.println("API Response: " + response.toString()); // Added logging
//            JsonNode features = response.get("features");
//
//            if (features != null && features.size() > 0) {
//                JsonNode bestMatch = features.get(0);
//                JsonNode center = bestMatch.get("center");
//                JsonNode placeName = bestMatch.get("place_name");
//
//                if (center != null && center.size() >= 2) {
//                    double lng = center.get(0).asDouble();
//                    double lat = center.get(1).asDouble();
//
//                    if (isWithinLagos(lat, lng)) {
//                        System.out.println("Found match: " + (placeName != null ? placeName.asText() : "N/A"));
//                        return new double[]{lat, lng};
//                    } else {
//                        throw new IOException("Location found but outside Lagos State boundaries.");
//                    }
//                } else {
//                    throw new IOException("No coordinates found in the API response.");
//                }
//            } else {
//                throw new IOException("No results found for '" + location + "' in Lagos, Nigeria.");
//            }
//        } catch (IOException e) {
//            throw new IOException("Geocoding request failed for '" + location + "': " + e.getMessage());
//        }
//    }
//
//    private static JsonNode executeRequest(String url) throws IOException {
//        try (CloseableHttpClient client = HttpClients.createDefault()) {
//            HttpGet request = new HttpGet(url);
//
//            try (CloseableHttpResponse response = client.execute(request)) {
//                String json = EntityUtils.toString(response.getEntity());
//                JsonNode node = new ObjectMapper().readTree(json);
//
//                if (response.getStatusLine().getStatusCode() != 200) {
//                    throw new IOException("HTTP " + response.getStatusLine().getStatusCode() +
//                            (node.has("message") ? ": " + node.get("message").asText() : ""));
//                }
//                return node;
//            }
//        }
//    }
//
//    private static boolean isWithinLagos(double lat, double lng) {
//        return lng >= LAGOS_BOUNDS[0] && lat >= LAGOS_BOUNDS[1] &&
//                lng <= LAGOS_BOUNDS[2] && lat <= LAGOS_BOUNDS[3];
//    }
//
//    public static void main(String[] args) throws IOException {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//
//        System.out.println("LAGOS STATE GEOCODER");
//        while(true){
//        System.out.println("=".repeat(30));
//        System.out.print("Enter a location in Lagos State: ");
//        String locationInput = reader.readLine();
//
//        if (locationInput != null && !locationInput.trim().isEmpty()) {
//            try {
//                double[] coordinates = getCoordinates(locationInput);
//                System.out.println("\nGeocoding Result:");
//                System.out.printf("Location: %s%n", locationInput);
//                System.out.printf("Latitude: %.6f%n", coordinates[0]);
//                System.out.printf("Longitude: %.6f%n", coordinates[1]);
//                System.out.println("Status: ✓ Success");
//            } catch (IOException e) {
//                System.out.println("\nGeocoding Failed:");
//                System.out.printf("Location: %s%n", locationInput);
//                System.out.println("Status: ✗ Failed: " + e.getMessage());
//            }
//        } else {
//            System.out.println("No location entered.");
//        }
//        }
//    }
//}



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
    private static final String ACCESS_TOKEN = "pk.eyJ1Ijoiby1jaHVrd3VrYXNpIiwiYSI6ImNtYXYwcTE2dTAwMXMya3M4NXo3Z2k1cHMifQ.cXZ0zN8loENSXW1-lj8-4g"; // Replace with your token
    private static final String COUNTRY = "ng"; // Nigeria
    private static final String PROXIMITY = "3.3792,6.5244";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Lagos Address Geocoder");
        System.out.println("-----------------------");
        System.out.println("Enter an address in Lagos (or 'quit' to exit):");

        while (true) {
            System.out.print("\nAddress: ");
            String address = scanner.nextLine();

            if (address.equalsIgnoreCase("quit")) {
                break;
            }

            try {
                double[] coordinates = geocodeAddress(address);
                System.out.printf("Coordinates: Latitude %.6f, Longitude %.6f%n",
                        coordinates[0], coordinates[1]);

                // Show Mapbox URL for verification
                System.out.println("Verify on Map: https://www.mapbox.com/search/?query=" +
                        URLEncoder.encode(address, StandardCharsets.UTF_8));
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                System.err.println("Try being more specific (include area/landmark)");
            }
        }

        scanner.close();
        System.out.println("Geocoding service ended");
    }

    public static double[] geocodeAddress(String address) throws IOException {
        String encodedAddress = URLEncoder.encode(address + ", Lagos", StandardCharsets.UTF_8);
        String requestUrl = String.format(
                "%s%s.json?country=%s&proximity=%s&access_token=%s",
                MAPBOX_API_URL, encodedAddress, COUNTRY, PROXIMITY, ACCESS_TOKEN
        );

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(requestUrl);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                return parseCoordinates(jsonResponse, address);
            }
        }
    }

    private static double[] parseCoordinates(String jsonResponse, String address) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonResponse);

        if (rootNode.has("message")) {
            throw new IOException("API Error: " + rootNode.get("message").asText());
        }

        JsonNode features = rootNode.get("features");
        if (features == null || features.size() == 0) {
            throw new IOException("No results found for: " + address);
        }

        JsonNode firstResult = features.get(0);
        JsonNode center = firstResult.get("center");
        if (center == null || center.size() < 2) {
            throw new IOException("Invalid coordinate data in response");
        }

        // Mapbox returns [longitude, latitude]
        return new double[]{center.get(1).asDouble(), center.get(0).asDouble()};
    }
}