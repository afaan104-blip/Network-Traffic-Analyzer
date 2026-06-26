package org.example;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;

import java.io.File;
import java.net.InetAddress;

public class GeoIPService {

    private static DatabaseReader reader;

    // LOAD DATABASE ONCE
    static {
        try {
            File dbFile = new File("GeoLite/GeoLite2-City.mmdb");
            reader = new DatabaseReader.Builder(dbFile).build();
            System.out.println("[GeoIP] Database loaded");
        } catch (Exception e) {
            System.err.println("[GeoIP] Failed to load database");
            e.printStackTrace();
        }
    }

    // RETURN LAT / LON
    public static double[] lookup(String ip) {
        try {
            if (isPrivateIP(ip)) return null;

            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = reader.city(ipAddress);

            Location loc = response.getLocation();

            if (loc.getLatitude() == null || loc.getLongitude() == null)
                return null;

            return new double[]{
                    loc.getLatitude(),
                    loc.getLongitude()
            };

        } catch (Exception e) {
            return null;
        }
    }

    // RETURN COUNTRY NAME
    public static String getCountry(String ip) {
        try {
            if (isPrivateIP(ip)) return "Local";

            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = reader.city(ipAddress);

            Country country = response.getCountry();
            if (country == null || country.getName() == null)
                return "Unknown";

            return country.getName();

        } catch (Exception e) {
            return "Unknown";
        }
    }

    // FILTER PRIVATE / LOCAL IPS
    private static boolean isPrivateIP(String ip) {
        return ip == null ||
                ip.startsWith("10.") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("172.16.") ||
                ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") ||
                ip.startsWith("172.19.") ||
                ip.startsWith("172.2") ||
                ip.startsWith("127.") ||
                ip.equals("::1");
    }
}