package Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

/**
 * Utility class for fetching the public IP address of the machine.
 * This class uses the ipify API (https://api.ipify.org) to retrieve the public
 * IP address.
 */
public class IpUtil {

    /**
     * Fetches the public IP address of the machine using the ipify API.
     *
     * @return A {@code String} representing the public IP address.
     *         If an error occurs (e.g., no internet connection or API is
     *         unreachable),
     *         it returns {@code "Unknown"}.
     */
    public static String getPublicIp() {
        String publicIp = "Unknown";
        try {
            // Create a URL object for the ipify API
            URL url = URI.create("https://api.ipify.org").toURL();

            // Open a stream to read the response from the API
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

            // Read the first line of the response, which contains the public IP
            publicIp = br.readLine().trim();
        } catch (IOException e) {
            // Log an error message if an exception occurs
            System.err.println("Error occurred while fetching public IP: " + e.getMessage());
        }
        return publicIp;
    }
}
