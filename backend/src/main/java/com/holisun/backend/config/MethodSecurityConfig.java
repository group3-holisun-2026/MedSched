package com.holisun.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Activeaza @PreAuthorize pe controllere (documents/module2/backend_module2_tasks.md,
 * sectiunea 0). Clasa separata de SecurityConfig ca sa nu intre in conflict cu
 * modificarile lui P3 pe partea de JWT/CORS.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
