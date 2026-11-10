package com.spartanclubcentral.Spartan.Club.repository;

import com.spartanclubcentral.Spartan.Club.model.Administrator;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for administrator accounts. */
public interface AdminRepository extends JpaRepository<Administrator, Long> {

    Optional<Administrator> findByEmailIgnoreCase(String email);
}
