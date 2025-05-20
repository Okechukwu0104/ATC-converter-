
# ATC Converter


A Java application that fetches geographic coordinates (latitude & longitude) for major transport points in Lagos, Nigeria, using the Mapbox Geocoding API.


## Documentation

[Mapbox Documentation](https://docs.mapbox.com/)


# Overview


This tool helps map Lagos's key transport locations (bus stops, junctions, and roundabouts) by querying the Mapbox Geocoding API to retrieve their coordinates.

# Key Features

Fetches latitude & longitude for Lagos transport points

Handles multiple query formats for better accuracy

Implements fallback mechanisms if initial search fails

Includes rate-limiting to avoid API throttling

Logs results in a readable format



## ⚙ Setup & Installation
Prerequisites
Java JDK 17+

Apache HttpClient (included in Maven dependencies)

Jackson Databind for JSON parsing

A valid Mapbox API access token





## Running Tests

To run tests, run the following command

```bash
  npm run test
```

# Installation
Clone the repository

bash
```
bash
git clone https://github.com/your-repo/lagos-transport-geocoder.git
```

bash
```
cd lagos-transport-geocoder
```
Update the Mapbox API token
Replace ACCESS_TOKEN in LagosGeocoder.java with your public Mapbox token :

java
```
private static final String ACCESS_TOKEN = "pk.your_public_token_here";
```

Run the application

bash
```
javac LagosGeocoder.java
java LagosGeocoder
```
(Or run via your preferred IDE like IntelliJ IDEA or Eclipse.)


🛠️ Usage
Default Transport Points
The app currently searches for these Lagos transport locations:

## Usage/Examples

```
Location Name           Area

Ojota Bus Stop	        Ojota
CMS Bus Stop	        Victoria Island
Oshodi Underbridge	    Oshodi
```

# Customizing Locations
Modify the TRANSPORT_POINTS map in LagosGeocoder.java to add or remove locations:

```java
static {
    TRANSPORT_POINTS.put("New Bus Stop", "New Area");
    // Add more as needed
}
```

# Output Format

```java
The program prints results in the following format:

[Location Name]          ([Area])        → Lat: [latitude], Lng: [longitude]
Example:

Ojota Bus Stop            (Ojota)         → Lat: 6.578700, Lng: 3.379200
```




# 🔍 How It Works

## Query Attempts

The app tries multiple query formats (full address, area-only, etc.) to maximize success.

If the first attempt fails, it falls back to simpler queries.


# Mapbox API Request

Uses HTTP GET requests to the Mapbox Geocoding API.

Filters results to Nigeria (country=ng) and biases toward Lagos (proximity=3.3792,6.5244).

# Error Handling

Logs errors if a location isn’t found.

Avoids rate-limiting with a 200ms delay between requests.

# 📜 License
This project is open-source under the MIT License.

# 🙏 Credits
Mapbox for the Geocoding API

Apache HttpClient for HTTP requests

Jackson for JSON parsing
