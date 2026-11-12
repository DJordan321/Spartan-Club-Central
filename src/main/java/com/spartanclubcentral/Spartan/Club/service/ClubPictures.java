package com.spartanclubcentral.Spartan.Club.service;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Picks the built-in picture for a category (FR-15), used by templates as
 * ${@clubPictures.categoryImage(name)}. The SVGs are original drawings in
 * static/images/categories/. Any other category gets default.svg.
 */
@Component("clubPictures")
public class ClubPictures {

    private static final Set<String> KNOWN = Set.of("academic", "cultural", "sports", "service");

    public String categoryImage(String categoryName) {
        String key = categoryName == null ? "" : categoryName.trim().toLowerCase(Locale.ROOT);
        return "/images/categories/" + (KNOWN.contains(key) ? key : "default") + ".svg";
    }
}
