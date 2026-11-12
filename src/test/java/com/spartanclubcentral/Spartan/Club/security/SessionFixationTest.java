package com.spartanclubcentral.Spartan.Club.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.repository.AuditLogRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.service.PasswordHasher;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * SPR-07: logging in must give the browser a NEW session cookie, so a session
 * ID someone saw before login (session fixation) is useless afterwards.
 *
 * Uses the real embedded web server and a real HTTP client with cookies,
 * exactly like a browser, because the simulated MockMvc session can't show
 * a cookie change.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionFixationTest {

    @LocalServerPort private int port;
    @Autowired private LeaderRepository leaders;
    @Autowired private AuditLogRepository auditLogs;
    @Autowired private PasswordHasher hasher;

    private final CookieManager cookies = new CookieManager();
    private final HttpClient http = HttpClient.newBuilder()
            .cookieHandler(cookies)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private ClubLeader leader;

    @BeforeEach
    void createLeader() {
        leader = new ClubLeader();
        leader.setName("Session Tester");
        leader.setEmail("session.tester@example.com");
        leader.setPasswordHash(hasher.hash("correct-horse-1"));
        leader = leaders.save(leader);
    }

    @AfterEach
    void cleanUp() {
        leaders.deleteById(leader.getLeaderId());
        auditLogs.deleteAll();
    }

    @Test
    void loginGivesTheBrowserANewSessionId() throws Exception {
        // 1. Open the login page: the server creates a session (it holds the CSRF token).
        HttpResponse<String> page = get("/login");
        String csrf = csrfToken(page.body());
        String before = sessionId();
        assertThat(before).isNotNull();

        // 2. Log in with the right password.
        String form = "_csrf=" + enc(csrf) + "&email=" + enc("session.tester@example.com")
                + "&password=" + enc("correct-horse-1");
        HttpResponse<String> login = http.send(HttpRequest.newBuilder(url("/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(login.headers().firstValue("Location")).hasValueSatisfying(l -> assertThat(l).endsWith("/dashboard"));

        // 3. The session cookie changed, and the new one opens the dashboard.
        String after = sessionId();
        assertThat(after).isNotNull().isNotEqualTo(before);
        assertThat(get("/dashboard").statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(url(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private String sessionId() {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("JSESSIONID")).map(HttpCookie::getValue).findFirst().orElse(null);
    }

    private URI url(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String csrfToken(String html) {
        Matcher m = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
        assertThat(m.find()).as("login page has a CSRF token").isTrue();
        return m.group(1);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
