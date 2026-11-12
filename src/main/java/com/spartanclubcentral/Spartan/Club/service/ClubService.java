package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.model.Category;
import com.spartanclubcentral.Spartan.Club.model.Club;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: rules for finding and showing clubs (FR-01, FR-02, FR-05).
 *
 * Controllers call this, never the repositories directly. Phase 5 adds
 * create/edit and the ownership check here.
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

    /**
     * A club for its public profile page. Inactive and unknown clubs are
     * treated the same ("not found"), so deactivated listings disappear (FR-12).
     */
    public Club getActiveClub(Long clubId) {
        if (clubId == null) {
            throw new NotFoundException("Club not found");
        }
        return clubRepository.findByClubIdAndActiveTrue(clubId)
                .orElseThrow(() -> new NotFoundException("Club not found"));
    }

    public List<Category> listCategories() {
        return categoryRepository.findAllByOrderByCategoryNameAsc();
    }

    /** The category with this id, if it exists (used to label filtered results). */
    public Optional<Category> findCategory(Integer categoryId) {
        return categoryId == null ? Optional.empty() : categoryRepository.findById(categoryId);
    }

    /**
     * Number of active clubs per category id, for the homepage buttons.
     * Counting in Java is fine for ~200 clubs and reuses the tested search query.
     */
    public Map<Integer, Long> activeClubCountsByCategory() {
        return clubRepository.search(null, null).stream()
                .collect(Collectors.groupingBy(c -> c.getCategory().getCategoryId(), Collectors.counting()));
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
