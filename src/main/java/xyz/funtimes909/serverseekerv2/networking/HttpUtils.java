package xyz.funtimes909.serverseekerv2.networking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpUtils {
    public static String run(String ip) {
        try (HttpClient client = HttpClient.newHttpClient()) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip + "?fields=status,message,continent,countryCode,org,as,reverse,query"))
                    .build();

            HttpResponse<String> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
            if (response.statusCode() == 429) return null;
            return response.body();
        }
    }

    public static String ipinfo(String ip) {
        try (HttpClient client = HttpClient.newHttpClient()) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipinfo.io/" + ip + "/json"))
                    .build();

            HttpResponse<String> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
            if (response.statusCode() == 429) return null;
            return response.body();
        }
    }

    public static void sendWebhook(String url, String player, String address) {
        String embed = "{\n" +
                "  \"embeds\": [\n" +
                "    {\n" +
                "      \"title\": \"Tracked player found on server!\",\n" +
                "      \"color\": 1127128,\n" +
                "      \"description\": \"**" + player + "** Was found on server [**" + address + "**]!\",\n" +
                "      \"author\": {\n" +
                "        \"name\": \"ServerSeekerV2\",\n" +
                "        \"url\": \"https://funtimes909.xyz/\",\n" +
                "        \"icon_url\": \"https://funtimes909.xyz/assets/images/serverseekerv2-icon-cropped.png\"\n" +
                "        \n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(embed))
                    .uri(URI.create(url))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
        }
    }
}
