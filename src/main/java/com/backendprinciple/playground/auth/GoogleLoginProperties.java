package com.backendprinciple.playground.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * "Continue with Google" settings ({@code app.google.*}). Leave the client id empty and the Google button
 * simply does not appear - the app works with email + password alone.
 *
 * @param publicUrl the site's public address, e.g. https://backend-playground.onrender.com. Google only
 *                  accepts the exact redirect URL registered in its console, so we build it from this
 *                  instead of trusting proxy headers. Empty = derive it from the request.
 */
@ConfigurationProperties(prefix = "app.google")
public record GoogleLoginProperties(String clientId, String clientSecret, String publicUrl) {

    public boolean enabled() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    public String redirectUri() {
        String base = publicUrl == null || publicUrl.isBlank() ? "{baseUrl}" : publicUrl.strip().replaceAll("/+$", "");
        return base + "/login/oauth2/code/{registrationId}";
    }
}
