package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.security.AccountPrincipal;
import com.spartanclubcentral.Spartan.Club.service.ClubImageService;
import com.spartanclubcentral.Spartan.Club.service.ClubService;
import com.spartanclubcentral.Spartan.Club.service.exception.ValidationException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller layer: club pictures (FR-15).
 *
 *   GET  /clubs/{id}/image                     the picture, visible clubs only (public)
 *   GET  /dashboard/clubs/{id}/picture         leader's picture page
 *   POST /dashboard/clubs/{id}/picture         upload/replace (multipart, CSRF token in the form)
 *   POST /dashboard/clubs/{id}/picture/remove  remove
 *
 * Pictures are addressed by club number only, never by file name or path.
 */
@Controller
public class PictureController {

    private final ClubImageService imageService;
    private final ClubService clubService;

    public PictureController(ClubImageService imageService, ClubService clubService) {
        this.imageService = imageService;
        this.clubService = clubService;
    }

    @GetMapping("/clubs/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable("id") Long id) {
        Path file = imageService.imageFileForPublicClub(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .body(new FileSystemResource(file));
    }

    @GetMapping("/dashboard/clubs/{id}/picture")
    public String page(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id, Model model) {
        return show(model, me, id, "", Map.of());
    }

    @PostMapping("/dashboard/clubs/{id}/picture")
    public String upload(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id,
                         @RequestParam(name = "file", required = false) MultipartFile file,
                         @RequestParam(name = "alt", required = false) String alt, Model model) throws IOException {
        try {
            imageService.upload(me.getId(), id, file == null ? null : file.getBytes(), alt);
            return "redirect:/dashboard/clubs/" + id + "/picture?uploaded";
        } catch (ValidationException e) {
            return show(model, me, id, alt, e.getFieldErrors());
        }
    }

    @PostMapping("/dashboard/clubs/{id}/picture/remove")
    public String remove(@AuthenticationPrincipal AccountPrincipal me, @PathVariable("id") Long id) {
        imageService.remove(me.getId(), id);
        return "redirect:/dashboard/clubs/" + id + "/picture?removed";
    }

    private String show(Model model, AccountPrincipal me, Long id, String alt, Map<String, String> errors) {
        model.addAttribute("club", clubService.requireOwnedClub(me.getId(), id));
        model.addAttribute("alt", alt == null ? "" : alt);
        model.addAttribute("errors", errors);
        return "dashboard/picture";
    }
}
