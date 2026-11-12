package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: club pictures (FR-15).
 *
 * Upload rules: JPEG or PNG only, checked by the file's first bytes (not its
 * name or the browser's label); at most 2 MB; not absurdly large in pixels;
 * alt text required (NFR-06). Every picture is decoded and RE-ENCODED as a
 * fresh JPEG at most 1200 px wide. That strips hidden data such as GPS
 * location (EXIF) and anything that isn't plain pixels.
 * Leaders can only change their own club's picture (ClubService.requireOwnedClub).
 */
@Service
@Transactional(readOnly = true)
public class ClubImageService {

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private final ClubService clubService;
    private final ClubRepository clubs;
    private final ImageStorage storage;
    private final AuditLogger audit;

    public ClubImageService(ClubService clubService, ClubRepository clubs, ImageStorage storage, AuditLogger audit) {
        this.clubService = clubService;
        this.clubs = clubs;
        this.storage = storage;
        this.audit = audit;
    }

    /** The picture file for a VISIBLE club, or NotFoundException (hidden clubs' pictures are private too). */
    public Path imageFileForPublicClub(Long clubId) {
        Club club = clubService.getActiveClub(clubId);
        Path file = storage.find(club.getImageFilename());
        if (file == null) {
            throw new NotFoundException("No picture");
        }
        return file;
    }

    /** Leader uploads or replaces their club's picture. */
    @Transactional
    public Club upload(Long leaderId, Long clubId, byte[] data, String rawAlt) {
        Club club = clubService.requireOwnedClub(leaderId, clubId);
        String alt = RequestValidator.trim(rawAlt);
        Map<String, String> errors = new LinkedHashMap<>();
        if (alt.isEmpty()) {
            errors.put("alt", "Describe the picture for people who can't see it.");
        } else if (alt.length() > FieldLimits.IMAGE_ALT) {
            errors.put("alt", "The description must be " + FieldLimits.IMAGE_ALT + " characters or fewer.");
        }
        byte[] jpeg = null;
        if (data == null || data.length == 0) {
            errors.put("file", "Choose a picture to upload.");
        } else if (data.length > FieldLimits.IMAGE_MAX_BYTES) {
            errors.put("file", "The picture must be 2 MB or smaller.");
        } else if (!startsWith(data, JPEG_MAGIC) && !startsWith(data, PNG_MAGIC)) {
            errors.put("file", "Upload a JPEG or PNG picture.");
        } else {
            try {
                jpeg = reencode(data);
            } catch (IllegalArgumentException e) {
                errors.put("file", e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        String old = club.getImageFilename();
        club.setImageFilename(storage.save(jpeg));
        club.setImageAlt(alt);
        club = clubs.save(club);
        storage.delete(old);
        audit.log("LEADER", leaderId, AuditLogger.CLUB_IMAGE_UPLOADED, "CLUB", clubId, true);
        return club;
    }

    /** Leader removes their club's picture; the category picture shows instead. */
    @Transactional
    public void remove(Long leaderId, Long clubId) {
        clear(clubService.requireOwnedClub(leaderId, clubId));
        audit.log("LEADER", leaderId, AuditLogger.CLUB_IMAGE_REMOVED, "CLUB", clubId, true);
    }

    /** Administrator removes an inappropriate picture from any club. */
    @Transactional
    public void removeAsAdmin(Long adminId, Long clubId) {
        clear(clubs.findById(clubId).orElseThrow(() -> new NotFoundException("Club not found")));
        audit.log("ADMIN", adminId, AuditLogger.CLUB_IMAGE_REMOVED, "CLUB", clubId, true);
    }

    /** Saves an already-safe picture made by the app itself (demo mode's logo). */
    @Transactional
    public void setGeneratedImage(Club club, BufferedImage image, String alt) {
        club.setImageFilename(storage.save(toJpeg(image)));
        club.setImageAlt(alt);
        clubs.save(club);
    }

    private void clear(Club club) {
        String old = club.getImageFilename();
        club.setImageFilename(null);
        club.setImageAlt(null);
        clubs.save(club);
        storage.delete(old);
    }

    /** Decode -> check size -> shrink to 1200 px wide -> fresh JPEG with no metadata. */
    static byte[] reencode(byte[] data) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (in == null || !readers.hasNext()) {
                throw new IllegalArgumentException("That file isn't a picture we can read. Upload a JPEG or PNG.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);                  // ignore metadata entirely
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > FieldLimits.IMAGE_MAX_SOURCE_PIXELS) {
                    throw new IllegalArgumentException("The picture is too large in size. Use one under 40 megapixels.");
                }
                BufferedImage source = reader.read(0);
                return toJpeg(source);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof IllegalArgumentException iae) {
                throw iae;
            }
            throw new IllegalArgumentException("That file isn't a picture we can read. Upload a JPEG or PNG.");
        }
    }

    private static byte[] toJpeg(BufferedImage source) {
        int width = Math.min(source.getWidth(), FieldLimits.IMAGE_MAX_WIDTH);
        int height = Math.max(1, (int) Math.round(source.getHeight() * (width / (double) source.getWidth())));
        BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);                                 // transparent PNG areas become white
        g.fillRect(0, 0, width, height);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.85f);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgb, null, null), param);   // null metadata: nothing copied over
            writer.dispose();
            ios.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("The picture couldn't be processed. Try another one.");
        }
    }

    private static boolean startsWith(byte[] data, byte[] magic) {
        if (data.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
