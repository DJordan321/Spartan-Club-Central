package com.spartanclubcentral.Spartan.Club.service;

import com.spartanclubcentral.Spartan.Club.repository.AdminRepository;
import com.spartanclubcentral.Spartan.Club.repository.CategoryRepository;
import com.spartanclubcentral.Spartan.Club.repository.ClubRepository;
import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import com.spartanclubcentral.Spartan.Club.repository.RequestRepository;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: reports whether the database is reachable and how many rows
 * each main table holds. Used only by the temporary Phase 1 checkpoint page.
 *
 * Returns counts only, never names, emails or passwords (NFR-02, SPR-02).
 */
@Service
@Transactional(readOnly = true)
public class SystemStatusService {

    private static final Logger log = LoggerFactory.getLogger(SystemStatusService.class);

    private final DataSource dataSource;
    private final ClubRepository clubs;
    private final CategoryRepository categories;
    private final LeaderRepository leaders;
    private final AdminRepository admins;
    private final RequestRepository requests;

    public SystemStatusService(DataSource dataSource, ClubRepository clubs, CategoryRepository categories,
                               LeaderRepository leaders, AdminRepository admins, RequestRepository requests) {
        this.dataSource = dataSource;
        this.clubs = clubs;
        this.categories = categories;
        this.leaders = leaders;
        this.admins = admins;
        this.requests = requests;
    }

    public Status currentStatus() {
        String database;
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            database = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        } catch (SQLException e) {
            // Details stay in the server log; the page only says "not connected" (SPR-06).
            log.warn("Database status check failed: {}", e.getMessage());
            return Status.notConnected();
        }
        long activeClubs = clubs.search(null, null).size();
        return new Status(true, database, clubs.count(), activeClubs, categories.count(),
                leaders.count(), admins.count(), requests.count());
    }

    /** Snapshot shown on the checkpoint page. */
    public record Status(boolean connected, String database, long totalClubs, long activeClubs,
                         long categories, long leaders, long admins, long requests) {

        static Status notConnected() {
            return new Status(false, null, 0, 0, 0, 0, 0, 0);
        }
    }
}
