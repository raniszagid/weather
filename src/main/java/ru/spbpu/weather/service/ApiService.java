package ru.spbpu.weather.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import ru.spbpu.weather.dto.WeatherDto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

@Service
@NoArgsConstructor
public class ApiService {
    public Optional<WeatherDto> makeRequest(String city) {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            String url = "http://goweather.xyz/weather/" + city;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            String json = response.body();
            ObjectMapper objectMapper = new ObjectMapper();
            return Optional.of(objectMapper.readValue(json, WeatherDto.class));
        } catch (IOException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}

