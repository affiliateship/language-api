package com.languageui.api.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOriginPatterns = List.of("*");
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("*");
    private List<String> exposedHeaders = List.of("Location", "Content-Type");
    private boolean allowCredentials = true;
    private Duration maxAge = Duration.ofHours(1);

    public List<String> getAllowedOriginPatterns() { return allowedOriginPatterns; }
    public void setAllowedOriginPatterns(List<String> value) { allowedOriginPatterns = value; }
    public List<String> getAllowedMethods() { return allowedMethods; }
    public void setAllowedMethods(List<String> value) { allowedMethods = value; }
    public List<String> getAllowedHeaders() { return allowedHeaders; }
    public void setAllowedHeaders(List<String> value) { allowedHeaders = value; }
    public List<String> getExposedHeaders() { return exposedHeaders; }
    public void setExposedHeaders(List<String> value) { exposedHeaders = value; }
    public boolean isAllowCredentials() { return allowCredentials; }
    public void setAllowCredentials(boolean value) { allowCredentials = value; }
    public Duration getMaxAge() { return maxAge; }
    public void setMaxAge(Duration value) { maxAge = value; }
}
