package com.backendprinciple.playground.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backendprinciple.playground.AbstractIntegrationTest;
import com.backendprinciple.playground.common.error.ApiException;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@EnabledIf("com.backendprinciple.playground.AbstractIntegrationTest#databaseAvailable")
@TestPropertySource(properties = {
        "app.google.client-id=test-client-id.apps.googleusercontent.com",
        "app.google.client-secret=test-secret",
        "app.google.public-url=https://learn.example.com/"
})
class GoogleLoginIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    AuthService authService;
    @Autowired
    JwtProperties jwt;
    @Autowired
    ObjectMapper json;

    private static String email(String name) {
        return name + "-" + UUID.randomUUID() + "@gmail.com";
    }

    @Test
    void theLoginPageIsToldGoogleIsAvailableAndTheButtonStartsTheOAuthFlow() throws Exception {
        mvc.perform(get("/api/auth/providers")).andExpect(jsonPath("$.google").value(true));

        String location = mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(location)
                .startsWith("https://accounts.google.com/o/oauth2/v2/auth")
                .contains("client_id=test-client-id.apps.googleusercontent.com")
                .contains("redirect_uri=https://learn.example.com/login/oauth2/code/google")
                .contains("scope=openid%20email%20profile")
                .contains("state=");
    }

    @Test
    void successfulGoogleLoginSetsOurRefreshCookieAndOpensTheDashboard() throws Exception {
        String email = email("asha");
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(60), Map.of(
                "sub", "google-" + UUID.randomUUID(), "email", email, "email_verified", true, "name", "Asha Rao"));
        var user = new DefaultOidcUser(List.of(), idToken);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        MockHttpServletResponse response = new MockHttpServletResponse();

        GoogleLoginConfig.onGoogleLogin(authService, jwt)
                .onAuthenticationSuccess(request, response, new OAuth2AuthenticationToken(user, List.of(), "google"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/dashboard");
        Cookie refresh = response.getCookie("pg_refresh");
        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();

        // The React app then refreshes as after any login.
        var body = json.readTree(mvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(body.get("user").get("email").asText()).isEqualTo(email);
        assertThat(body.get("user").get("displayName").asText()).isEqualTo("Asha Rao");
        assertThat(body.get("user").get("googleLinked").asBoolean()).isTrue();
        assertThat(body.get("user").get("hasPassword").asBoolean()).isFalse();
    }

    @Test
    void googleOnlyAccountsCanSetAPasswordLater() throws Exception {
        String email = email("ravi");
        AuthService.Session s = authService.loginWithGoogle("sub-" + UUID.randomUUID(), email, true, "Ravi");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"anything1\"}".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Continue with Google")));

        mvc.perform(put("/api/me/password").header("Authorization", "Bearer " + s.accessToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newPassword\":\"new-password-1\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"new-password-1\"}".formatted(email)))
                .andExpect(status().isOk());
    }

    @Test
    void theSameGoogleAccountAlwaysReturnsToTheSameUser() {
        String sub = "sub-" + UUID.randomUUID();
        AuthService.Session first = authService.loginWithGoogle(sub, email("meera"), true, "Meera");
        // Even if the Gmail address changes, the stable "sub" finds the same account.
        AuthService.Session again = authService.loginWithGoogle(sub, email("meera-new"), true, "Meera");
        assertThat(again.user().id()).isEqualTo(first.user().id());
    }

    @Test
    void linkingAnExistingPasswordAccountLocksOutWhoeverRegisteredTheEmailFirst() throws Exception {
        String email = email("tara");
        var registered = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\",\"displayName\":\"Tara\"}".formatted(email)))
                .andExpect(status().isCreated()).andReturn().getResponse();
        Cookie oldSession = registered.getCookie("pg_refresh");
        String userId = json.readTree(registered.getContentAsString()).get("user").get("id").asText();

        AuthService.Session google = authService.loginWithGoogle("sub-" + UUID.randomUUID(), email.toUpperCase(), true, "Tara G");

        assertThat(google.user().id().toString()).isEqualTo(userId);    // same account, progress kept
        mvc.perform(post("/api/auth/refresh").cookie(oldSession)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unverifiedGoogleEmailsAreRejected() {
        assertThatThrownBy(() -> authService.loginWithGoogle("sub-" + UUID.randomUUID(), email("x"), false, "X"))
                .isInstanceOf(ApiException.class);
    }
}
