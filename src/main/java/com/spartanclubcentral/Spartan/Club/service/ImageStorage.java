package com.spartanclubcentral.Spartan.Club.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service layer: stores club pictures as files in UPLOAD_DIR (FR-15).
 *
 * The folder is outside the code and outside static/, so files are only
 * reachable through GET /clubs/{id}/image. File names are always made here
 * (a random UUID + ".jpg") and checked against a strict pattern, so a name
 * from anywhere else can never point outside the folder.
 */
@Component
public class ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);
    private static final Pattern SAFE_NAME = Pattern.compile("^[0-9a-f-]{36}\\.jpg$");

    private final Path root;

    public ImageStorage(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /** Saves JPEG bytes under a new random name and returns that name. */
    public String save(byte[] jpeg) {
        String name = UUID.randomUUID() + ".jpg";
        try {
            Files.createDirectories(root);
            Files.write(resolve(name), jpeg);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save picture", e);
        }
        return name;
    }

    /** The file for a stored name, or null if the name isn't one we made or the file is gone. */
    public Path find(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            return null;
        }
        Path file = resolve(name);
        return Files.isRegularFile(file) ? file : null;
    }

    /** Deletes a stored picture; quietly ignores names we didn't make and missing files. */
    public void delete(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(name));
        } catch (IOException e) {
            log.warn("Could not delete a club picture: {}", e.getClass().getSimpleName());
        }
    }

    public Path root() {
        return root;
    }

    private Path resolve(String name) {
        Path file = root.resolve(name).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Bad picture name");
        }
        return file;
    }
}
