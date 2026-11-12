package com.spartanclubcentral.Spartan.Club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for picture uploads (FR-15): file checks, re-encoding, storage. */
public class ClubImageServiceTest {

    @TempDir Path dir;

    private final ClubService clubService = mock(ClubService.class);
    private final ClubRepository clubs = mock(ClubRepository.class);
    private ImageStorage storage;
    private ClubImageService service;
    private Club club;

    @BeforeEach
    void setUp() {
        storage = new ImageStorage(dir.toString());
        service = new ClubImageService(clubService, clubs, storage, mock(AuditLogger.class));
        club = new Club();
        club.setClubName("Chess Club");
        club.setCategory(new Category("Academic"));
        club.setActive(true);
        when(clubService.requireOwnedClub(1L, 7L)).thenReturn(club);
        when(clubService.getActiveClub(7L)).thenReturn(club);
        when(clubs.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void jpegIsResizedReencodedAndStored() throws Exception {
        service.upload(1L, 7L, image("jpeg", 3000, 2000), "Members at the fall tournament");
        assertThat(club.getImageFilename()).matches("[0-9a-f-]{36}\\.jpg");
        assertThat(club.getImageAlt()).isEqualTo("Members at the fall tournament");
        BufferedImage saved = ImageIO.read(Files.newInputStream(storage.find(club.getImageFilename())));
        assertThat(saved.getWidth()).isEqualTo(FieldLimits.IMAGE_MAX_WIDTH);
        assertThat(saved.getHeight()).isEqualTo(800);
        assertThat(service.imageFileForPublicClub(7L)).exists();
    }

    @Test
    void transparentPngBecomesJpeg() throws Exception {
        service.upload(1L, 7L, image("png", 400, 300), "Logo");
        byte[] saved = Files.readAllBytes(storage.find(club.getImageFilename()));
        assertThat(saved[0]).isEqualTo((byte) 0xFF);
        assertThat(saved[1]).isEqualTo((byte) 0xD8);
    }

    @Test
    void hiddenDataSuchAsGpsIsRemoved() throws Exception {
        byte[] withExif = addExifSegment(image("jpeg", 200, 100), "GPSLatitude 36.8508 GPSLongitude -76.2859");
        assertThat(new String(withExif, StandardCharsets.ISO_8859_1)).contains("Exif", "GPSLatitude");
        service.upload(1L, 7L, withExif, "Photo");
        String saved = new String(Files.readAllBytes(storage.find(club.getImageFilename())), StandardCharsets.ISO_8859_1);
        assertThat(saved).doesNotContain("Exif").doesNotContain("GPSLatitude");
    }

    @Test
    void rejectsFilesThatArentReallyJpegOrPng() throws Exception {
        expectFileError("not a picture".getBytes(StandardCharsets.UTF_8), "Upload a JPEG or PNG picture.");
        expectFileError(image("bmp", 50, 50), "Upload a JPEG or PNG picture.");
        byte[] fakeJpeg = new byte[100];
        fakeJpeg[0] = (byte) 0xFF; fakeJpeg[1] = (byte) 0xD8; fakeJpeg[2] = (byte) 0xFF;   // right first bytes, junk after
        expectFileError(fakeJpeg, "That file isn't a picture we can read. Upload a JPEG or PNG.");
        expectFileError(new byte[0], "Choose a picture to upload.");
        expectFileError(new byte[(int) FieldLimits.IMAGE_MAX_BYTES + 1], "The picture must be 2 MB or smaller.");
        assertThat(club.getImageFilename()).isNull();
        try (var files = Files.list(dir)) { assertThat(files.count()).isZero(); }
    }

    @Test
    void altTextIsRequired() throws Exception {
        assertThatThrownBy(() -> service.upload(1L, 7L, image("png", 10, 10), "  "))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).getFieldErrors()).containsOnlyKeys("alt"));
    }

    @Test
    void replacingDeletesTheOldFileAndRemovingClearsIt() throws Exception {
        service.upload(1L, 7L, image("jpeg", 100, 100), "First");
        String first = club.getImageFilename();
        service.upload(1L, 7L, image("jpeg", 100, 100), "Second");
        assertThat(storage.find(first)).isNull();
        service.remove(1L, 7L);
        assertThat(club.getImageFilename()).isNull();
        try (var files = Files.list(dir)) { assertThat(files.count()).isZero(); }
        assertThatThrownBy(() -> service.imageFileForPublicClub(7L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void storageRefusesNamesItDidNotMake() {
        assertThat(storage.find("../../etc/passwd")).isNull();
        assertThat(storage.find("notes.txt")).isNull();
        storage.delete("../../anything");   // quietly ignored
    }

    private void expectFileError(byte[] data, String message) {
        assertThatThrownBy(() -> service.upload(1L, 7L, data, "Alt"))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).getFieldErrors()).containsEntry("file", message));
    }

    public static byte[] image(String format, int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, format.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0, 87, 63));
        g.fillRect(0, 0, w / 2, h);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    /** Inserts an APP1 "Exif" segment right after the JPEG start marker, like a phone camera does. */
    static byte[] addExifSegment(byte[] jpeg, String text) throws Exception {
        byte[] payload = ("Exif\0\0" + text).getBytes(StandardCharsets.ISO_8859_1);
        int len = payload.length + 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2);
        out.write(new byte[] {(byte) 0xFF, (byte) 0xE1, (byte) (len >> 8), (byte) len});
        out.write(payload);
        out.write(jpeg, 2, jpeg.length - 2);
        assertThat(ImageIO.read(new ByteArrayInputStream(out.toByteArray()))).isNotNull();
        return out.toByteArray();
    }
}
