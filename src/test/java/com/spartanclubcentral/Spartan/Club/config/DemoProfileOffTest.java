package com.spartanclubcentral.Spartan.Club.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.spartanclubcentral.Spartan.Club.repository.LeaderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/** Without the "demo" profile there is no demo seeder and no demo account. */
@SpringBootTest
class DemoProfileOffTest {

    @Autowired private ApplicationContext context;
    @Autowired private LeaderRepository leaders;

    @Test
    void noDemoLeaderWithoutTheDemoProfile() {
        assertThat(context.getBeanNamesForType(DemoLeaderSeeder.class)).isEmpty();
        assertThat(leaders.findByEmailIgnoreCase(DemoLeaderSeeder.EMAIL)).isEmpty();
    }
}
