package com.zoho.eservemcp.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ZohoTokenManager: Handles Zoho OAuth token fetching, refresh, rotation, and persistence.
 * Uses a persistable file for organizational refresh token storage (never hard-code in source).
 */
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
    private static final long EXPIRY_BUFFER = 60; // Seconds before expiry to proactively rotate

    private final RestTemplate restTemplate = new RestTemplate();
    private final ReentrantLock refreshLock = new ReentrantLock();

    /**
     * Initializes the refresh token from file at startup.
     * Throws error and halts app if not available.
     */
    @PostConstruct
    public void init() {
        Path path = Paths.get(refreshTokenFilePath);
        try {
            if (Files.exists(path)) {
                refreshToken = Files.readString(path, StandardCharsets.UTF_8).trim();
                if (refreshToken.isEmpty()) {
                    log.error("Refresh token loaded from {} is blank!", refreshTokenFilePath);
                    throw new RuntimeException("Refresh token is blank—cannot start Zoho integration");
                }
                log.info("Loaded Zoho refresh token from {}", refreshTokenFilePath);
            } else {
                log.error("Refresh token file missing at {}. Please provide a valid refresh token file.", refreshTokenFilePath);
                throw new RuntimeException("Refresh token file missing – cannot start Zoho integration");
            }
        } catch (IOException ioe) {
            log.error("I/O error loading Zoho refresh token: {}", ioe.getMessage(), ioe);
            throw new RuntimeException("Failed to load Zoho refresh token file", ioe);
        } catch (Exception e) {
            log.error("Error loading Zoho refresh token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load Zoho refresh token", e);
        }
    }

    /**
     * Returns a valid access token, refreshing if near expiry or missing.
     * @return Non-null, non-empty Zoho access token, throws on failure.
     */
    public synchronized String getValidAccessToken() {
        if (currentAccessToken.get() == null || expiresAt.minusSeconds(EXPIRY_BUFFER).isBefore(Instant.now())) {
            refreshAccessToken();
        }
        String token = currentAccessToken.get();
        if (token == null || token.isEmpty()) {
            log.error("Access token is null or empty after refresh.");
            throw new IllegalStateException("Zoho access token is null or empty (NPE).");
        }
        return token;
    }

    /**
     * Safely (with locking) refreshes and persists tokens as needed.
     */
    private void refreshAccessToken() {
        refreshLock.lock();
        try {
            if (refreshToken == null || refreshToken.isEmpty()) {
                log.error("Refresh token is missing. Cannot proceed with access token refresh.");
                throw new IllegalStateException("Refresh token is missing.");
            }

            log.info("Refreshing Zoho access token using refresh token: {}", mask(refreshToken));

            // Build the URL with query parameters, per Zoho official docs
            String url = TOKEN_URL
                    + "?refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&grant_type=refresh_token";

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response;
            try {
                response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        Map.class
                );
            } catch (RestClientException ex) {
                log.error("HTTP error while refreshing Zoho token: {}", ex.getMessage(), ex);
                throw new RuntimeException("HTTP error while refreshing Zoho token: " + ex.getMessage(), ex);
            }

            Map result = response.getBody();
            if (result == null) {
                log.error("Zoho token API returned null response body.");
                throw new RuntimeException("Failed to refresh Zoho token: null response body.");
            }

            // Defensive checks on expected fields
            Object rawAccessToken = result.get("access_token");
            if (rawAccessToken == null || rawAccessToken.toString().isEmpty()) {
                log.error("No access_token in Zoho token response: {}", result);
                throw new RuntimeException("No access_token present in the Zoho response.");
            }
            String accessToken = rawAccessToken.toString().trim();

            Object rawExpiresIn = result.get("expires_in");
            int expiresIn = 3600;
            if (rawExpiresIn != null) {
                try {
                    expiresIn = Integer.parseInt(rawExpiresIn.toString());
                } catch (NumberFormatException nfe) {
                    log.warn("Invalid expires_in value '{}', using default 3600s.", rawExpiresIn);
                }
            } else {
                log.warn("expires_in missing in response, using default 3600s");
            }

            currentAccessToken.set(accessToken);
            expiresAt = Instant.now().plusSeconds(expiresIn);

            log.info("Zoho access token refreshed, expires in {} seconds (at {}).", expiresIn, expiresAt);

            // Do token rotation logic if present
            Object rawRefreshToken = result.get("refresh_token");
            if (rawRefreshToken != null) {
                String newRefreshToken = rawRefreshToken.toString().trim();
                if (!newRefreshToken.isEmpty() && !newRefreshToken.equals(refreshToken)) {
                    persistRefreshTokenToFile(newRefreshToken);
                    refreshToken = newRefreshToken;
                    log.info("Zoho returned a new refresh token (rotated). Saved to file {}", refreshTokenFilePath);
                }
            }
        } catch (Exception e) {
            log.error("Exception during Zoho token refresh: {}", e.getMessage(), e.getCause());
            throw new RuntimeException("Zoho token refresh failed.", e);
        } finally {
            refreshLock.unlock();
        }
    }

    /**
     * Persists a new refresh token to disk. Fails fast if not writable.
     */
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
            log.error("Failed to persist Zoho refresh token to file: {}. Cause: {}", refreshTokenFilePath, e.getMessage());
            throw new RuntimeException("Could not save new Zoho refresh token", e);
        }
    }

    /**
     * Utility to mask sensitive tokens for log output.
     */
    private String mask(String token) {
        if (token == null || token.length() < 6) return "****";
        return token.substring(0, 3) + "****" + token.substring(token.length() - 3);
    }
}