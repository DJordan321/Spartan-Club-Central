package com.spartanclubcentral.Spartan.Club.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.model.ClubLeader;
import com.spartanclubcentral.Spartan.Club.repository.AuditLogRepository;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal.Role;
import com.spartanclubcentral.Spartan.Club.service.AuditLogger;
import com.spartanclubcentral.Spartan.Club.service.ClubImageServiceTest;
import com.spartanclubcentral.Spartan.Club.service.ImageStorage;
import jakarta.persistence.EntityManager;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Club pictures through the web layer (FR-15, SPR-03, NFR-06). Files go to a temporary folder. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PictureControllerTest {

    @TempDir static Path uploads;

    @DynamicPropertySource
    static void uploadDir(DynamicPropertyRegistry registry) {
        registry.add("app.upload-dir", () -> uploads.toString());
    }

    @Autowired private MockMvc mvc;
    @Autowired private ClubRepository clubs;
    @Autowired private CategoryRepository categories;
    @Autowired private LeaderRepository leaders;
    @Autowired private AuditLogRepository auditLogs;
    @Autowired private ImageStorage storage;
    @Autowired private EntityManager em;

    private Club chess;
    private Club soccer;
    private ClubLeader leaderA;
    private ClubLeader leaderB;

    @BeforeEach
    void setUp() {
        Category academic = categories.save(new Category("Academic"));
        Category sports = categories.save(new Category("Sports"));
        chess = clubs.save(club("Chess Club", academic));
        soccer = clubs.save(club("Soccer Club", sports));
        leaderA = leaders.save(leader("a@example.com", chess));
        leaderB = leaders.save(leader("b@example.com", soccer));
    }

    @Test
    @DisplayName("Without an upload, pages show the category picture")
    void categoryPictureByDefault() throws Exception {
        mvc.perform(get("/clubs/" + chess.getClubId())).andExpect(content().string(containsString("/images/categories/academic.svg")));
        mvc.perform(get("/clubs")).andExpect(content().string(allOf(containsString("/images/categories/academic.svg"),
                containsString("/images/categories/sports.svg"))));
        mvc.perform(get("/")).andExpect(content().string(containsString("/images/categories/sports.svg")));
        mvc.perform(get("/images/categories/academic.svg")).andExpect(status().isOk());
        mvc.perform(get("/clubs/" + chess.getClubId() + "/image")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Leader uploads a picture: it's served as JPEG with its alt text on the club page")
    void uploadAndServe() throws Exception {
        mvc.perform(upload(chess, leaderA, ClubImageServiceTest.image("png", 1600, 900), "Chess Club logo"))
                .andExpect(redirectedUrl("/dashboard/clubs/" + chess.getClubId() + "/picture?uploaded"));
        em.flush(); em.clear();
        Club saved = clubs.findById(chess.getClubId()).orElseThrow();
        assertThat(storage.find(saved.getImageFilename())).isNotNull();

        mvc.perform(get("/clubs/" + chess.getClubId() + "/image"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")));
        mvc.perform(get("/clubs/" + chess.getClubId())).andExpect(content().string(allOf(
                containsString("alt=\"Chess Club logo\""), containsString("/clubs/" + chess.getClubId() + "/image?v="))));
        assertThat(auditLogs.findAll()).anyMatch(a -> a.getAction().equals(AuditLogger.CLUB_IMAGE_UPLOADED));
    }

    @Test
    @DisplayName("Bad uploads show messages next to the fields and save nothing")
    void badUploads() throws Exception {
        mvc.perform(upload(chess, leaderA, "hello".getBytes(), "Picture"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Upload a JPEG or PNG picture.")));
        mvc.perform(upload(chess, leaderA, ClubImageServiceTest.image("jpeg", 50, 50), ""))
                .andExpect(content().string(containsString("Describe the picture for people who can")));
        mvc.perform(upload(chess, leaderA, new byte[2 * 1024 * 1024 + 10], "Too big"))
                .andExpect(content().string(allOf(containsString("The picture must be 2 MB or smaller."), containsString("value=\"Too big\""))));
        em.flush(); em.clear();
        assertThat(clubs.findById(chess.getClubId()).orElseThrow().getImageFilename()).isNull();
    }

    @Test
    @DisplayName("Hidden club: its picture isn't served; removing it brings back the category picture")
    void hiddenAndRemove() throws Exception {
        mvc.perform(upload(chess, leaderA, ClubImageServiceTest.image("jpeg", 80, 80), "Logo")).andExpect(status().is3xxRedirection());
        em.flush(); em.clear();
        String file = clubs.findById(chess.getClubId()).orElseThrow().getImageFilename();
        mvc.perform(post("/dashboard/clubs/" + chess.getClubId() + "/deactivate").with(csrf()).with(user(me(leaderA))));
        mvc.perform(get("/clubs/" + chess.getClubId() + "/image")).andExpect(status().isNotFound());
        mvc.perform(post("/dashboard/clubs/" + chess.getClubId() + "/picture/remove").with(csrf()).with(user(me(leaderA))))
                .andExpect(redirectedUrl("/dashboard/clubs/" + chess.getClubId() + "/picture?removed"));
        assertThat(storage.find(file)).isNull();
    }

    @Test
    @DisplayName("SPR-03: Leader A can't view, upload or remove Soccer Club's picture")
    void ownership() throws Exception {
        mvc.perform(upload(soccer, leaderB, ClubImageServiceTest.image("jpeg", 80, 80), "Soccer logo")).andExpect(status().is3xxRedirection());
        long before = denials();
        mvc.perform(get("/dashboard/clubs/" + soccer.getClubId() + "/picture").with(user(me(leaderA)))).andExpect(status().isForbidden());
        mvc.perform(upload(soccer, leaderA, ClubImageServiceTest.image("jpeg", 80, 80), "Hacked")).andExpect(status().isForbidden());
        mvc.perform(post("/dashboard/clubs/" + soccer.getClubId() + "/picture/remove").with(csrf()).with(user(me(leaderA))))
                .andExpect(status().isForbidden());
        em.flush(); em.clear();
        assertThat(clubs.findById(soccer.getClubId()).orElseThrow().getImageAlt()).isEqualTo("Soccer logo");
        assertThat(denials() - before).isEqualTo(3);
    }

    @Test
    @DisplayName("Upload needs the CSRF token; tricky image addresses don't reach files")
    void guards() throws Exception {
        mvc.perform(multipart("/dashboard/clubs/" + chess.getClubId() + "/picture")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", ClubImageServiceTest.image("jpeg", 10, 10)))
                        .param("alt", "x").with(user(me(leaderA))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/clubs/abc/image")).andExpect(status().isNotFound());
        mvc.perform(get("/clubs/" + chess.getClubId() + "/image/..%2F..%2Fetc%2Fpasswd")).andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Deleting a club deletes its picture file; the admin can remove a picture")
    void deleteClubAndAdmin() throws Exception {
        mvc.perform(upload(chess, leaderA, ClubImageServiceTest.image("jpeg", 80, 80), "Logo")).andExpect(status().is3xxRedirection());
        mvc.perform(upload(soccer, leaderB, ClubImageServiceTest.image("jpeg", 80, 80), "Logo")).andExpect(status().is3xxRedirection());
        em.flush(); em.clear();
        String chessFile = clubs.findById(chess.getClubId()).orElseThrow().getImageFilename();
        String soccerFile = clubs.findById(soccer.getClubId()).orElseThrow().getImageFilename();

        mvc.perform(post("/dashboard/clubs/" + chess.getClubId() + "/delete").with(csrf()).with(user(me(leaderA))))
                .andExpect(redirectedUrl("/dashboard?deleted"));
        assertThat(storage.find(chessFile)).isNull();

        AccountPrincipal admin = new AccountPrincipal(1L, "admin@example.com", "Administrator", "x", Role.ADMIN, true);
        mvc.perform(get("/admin").with(user(admin))).andExpect(content().string(containsString("Remove picture")));
        mvc.perform(post("/admin/clubs/" + soccer.getClubId() + "/remove-picture").with(csrf()).with(user(admin)))
                .andExpect(redirectedUrl("/admin?pictureRemoved#clubs"));
        assertThat(storage.find(soccerFile)).isNull();
        assertThat(auditLogs.findAll()).anyMatch(a -> a.getActorType().equals("ADMIN") && a.getAction().equals(AuditLogger.CLUB_IMAGE_REMOVED));
    }

    private RequestBuilder upload(Club club, ClubLeader who, byte[] data, String alt) {
        return multipart("/dashboard/clubs/" + club.getClubId() + "/picture")
                .file(new MockMultipartFile("file", "photo.jpg", "image/jpeg", data))
                .param("alt", alt).with(csrf()).with(user(me(who)));
    }

    private static AccountPrincipal me(ClubLeader l) {
        return new AccountPrincipal(l.getLeaderId(), l.getEmail(), "Leader", "x", Role.LEADER, true);
    }

    private long denials() {
        return auditLogs.findAll().stream().filter(l -> l.getAction().equals(AuditLogger.OWNERSHIP_DENIED)).count();
    }

    private static Club club(String name, Category category) {
        Club c = new Club();
        c.setClubName(name);
        c.setDescription("About " + name);
        c.setCategory(category);
        c.setContactEmail("contact@example.com");
        return c;
    }

    private static ClubLeader leader(String email, Club club) {
        ClubLeader l = new ClubLeader();
        l.setName("Leader " + email);
        l.setEmail(email);
        l.setPasswordHash("$2a$04$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz01234");
        l.setClub(club);
        return l;
    }
}
