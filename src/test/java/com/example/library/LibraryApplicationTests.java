package com.example.library;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Boots the full Spring context (H2 in-memory + seed data) to catch wiring problems. */
@SpringBootTest
class LibraryApplicationTests {

    @Test
    void contextLoads() {
    }
}
