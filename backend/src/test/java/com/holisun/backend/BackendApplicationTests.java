package com.holisun.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Profilul `test` tine testele pe medsched_test (create-drop, fara Flyway). Fara el testul
// ruleaza pe profilul implicit `dev`, adica pe baza de dezvoltare a fiecaruia — nedeterminist,
// si intra in conflict cu migratiile Flyway.
@ActiveProfiles("test")
@SpringBootTest
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
