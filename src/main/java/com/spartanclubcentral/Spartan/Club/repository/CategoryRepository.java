package com.spartanclubcentral.Spartan.Club.repository;

import com.spartanclubcentral.Spartan.Club.model.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for categories (filter dropdown and homepage buttons, FR-02, NFR-03). */
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    List<Category> findAllByOrderByCategoryNameAsc();
}
