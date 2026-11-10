package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: rules for finding clubs (FR-01, FR-02).
 *
 * Controllers call this, never the repositories directly. Phase 2 adds the
 * profile lookup; Phase 5 adds create/edit and the ownership check.
 */
@Service
@Transactional(readOnly = true)
public class ClubService {

    private final ClubRepository clubRepository;
    private final CategoryRepository categoryRepository;

    public ClubService(ClubRepository clubRepository, CategoryRepository categoryRepository) {
        this.clubRepository = clubRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * One search path for every keyword/category combination.
     * Blank keyword means "no keyword"; very long input is cut to FieldLimits.SEARCH.
     */
    public List<Club> search(String rawKeyword, Integer categoryId) {
        return clubRepository.search(normalizeKeyword(rawKeyword), categoryId);
    }

    public List<Category> listCategories() {
        return categoryRepository.findAllByOrderByCategoryNameAsc();
    }

    /** Trims the keyword; returns null when there is nothing to search for. */
    static String normalizeKeyword(String rawKeyword) {
        if (rawKeyword == null || rawKeyword.isBlank()) {
            return null;
        }
        String kw = rawKeyword.trim();
        return kw.length() > FieldLimits.SEARCH ? kw.substring(0, FieldLimits.SEARCH) : kw;
    }
}
