package com.zoho.eservemcp.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class ZohoTokenManager {
    @Value("${zoho.api.client-id}")
    private String clientId;
    @Value("${zoho.api.client-secret}")
    private String clientSecret;
    @Value("${zoho.api.refresh-token-file:./zoho_refresh_token.txt}")
    private String refreshTokenFilePath;

    private volatile String refreshToken;

    private final AtomicReference<String> currentAccessToken = new AtomicReference<>();
    private Instant expiresAt = Instant.now();

    private static final String TOKEN_URL = "https://accounts.zoho.in/oauth/v2/token";
    private static final long EXPIRY_BUFFER = 60; // Seconds before expiry to refresh

    private final RestTemplate restTemplate = new RestTemplate();
    private final ReentrantLock refreshLock = new ReentrantLock();

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(refreshTokenFilePath);
            if (Files.exists(path)) {
                refreshToken = Files.readString(path, StandardCharsets.UTF_8).trim();
                log.info("Loaded Zoho refresh token from {}", refreshTokenFilePath);
            } else {
                log.error("Refresh token file missing at {}. Please provide a valid refresh token file.", refreshTokenFilePath);
                throw new RuntimeException("Refresh token file missing – cannot start Zoho integration");
            }
        } catch (Exception e) {
            log.error("Error loading Zoho refresh token file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load Zoho refresh token file", e);
        }
    }

    public synchronized String getValidAccessToken() {
        if (currentAccessToken.get() == null || expiresAt.minusSeconds(EXPIRY_BUFFER).isBefore(Instant.now())) {
            refreshAccessToken();
        }
        String token = currentAccessToken.get();
        if (token == null || token.isEmpty()) {
            log.error("Access token NPE or empty after refresh. Cannot proceed.");
            throw new IllegalStateException("Zoho access token is null or empty.");
        }
        return token;
    }

    private void refreshAccessToken() {
        refreshLock.lock();
        try {
            if (refreshToken == null || refreshToken.isEmpty()) {
                log.error("Cannot refresh access token: refresh token is null or empty.");
                throw new IllegalStateException("Refresh token is missing.");
            }

            log.info("Refreshing Zoho access token...");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "refresh_token=" + refreshToken +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&grant_type=refresh_token";

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response;
            try {
                response = restTemplate.exchange(
                        TOKEN_URL,
                        HttpMethod.POST,
                        entity,
                        Map.class
                );
            } catch (RestClientException ex) {
                log.error("HTTP error while refreshing Zoho token: {}", ex.getMessage(), ex);
                throw new RuntimeException("HTTP error while refreshing Zoho token", ex);
            }

            Map result = response.getBody();
            if (result == null) {
                log.error("Zoho token refresh response was null.");
                throw new RuntimeException("Failed to refresh Zoho token (null response).");
            }

            Object rawAccessToken = result.get("access_token");
            Object rawExpiresIn = result.get("expires_in");
            if (rawAccessToken == null || rawAccessToken.toString().isEmpty()) {
                log.error("Access token not present in Zoho refresh response: {}", result);
                throw new RuntimeException("No access token present in Zoho response.");
            }
            if (rawExpiresIn == null) {
                log.error("expires_in missing in Zoho response, using default 3600s");
            }

            String accessToken = rawAccessToken.toString().trim();
            int expiresIn = 3600;
            try {
                expiresIn = rawExpiresIn != null ? Integer.parseInt(rawExpiresIn.toString()) : 3600;
            } catch (NumberFormatException e) {
                log.warn("Invalid expires_in value: {} (using default 3600)", rawExpiresIn);
            }

            currentAccessToken.set(accessToken);
            expiresAt = Instant.now().plusSeconds(expiresIn);

            log.info("Zoho access token refreshed, expires in {} seconds (at {})", expiresIn, expiresAt);

            // If Zoho returns a new refresh token, update file and memory.
            Object rawRefreshToken = result.get("refresh_token");
            if (rawRefreshToken != null) {
                String newRefreshToken = rawRefreshToken.toString().trim();
                if (!newRefreshToken.isEmpty() && !newRefreshToken.equals(refreshToken)) {
                    persistRefreshTokenToFile(newRefreshToken);
                    refreshToken = newRefreshToken;
                    log.info("Zoho returned a new refresh token. Persisted to file {}", refreshTokenFilePath);
                }
            }
        } catch (Exception e) {
            log.error("Exception during Zoho token refresh: {}", e.getMessage(), e);
            throw new RuntimeException("Zoho token refresh failed.", e);
        } finally {
            refreshLock.unlock();
        }
    }

    private void persistRefreshTokenToFile(String newRefreshToken) {
        try {
            Files.writeString(
                    Paths.get(refreshTokenFilePath),
                    newRefreshToken,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            log.error("Failed to persist Zoho refresh token to file: {}", refreshTokenFilePath, e);
            throw new RuntimeException("Could not save new refresh token", e);
        }
    }
}