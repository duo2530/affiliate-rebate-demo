package com.techplant.affiliate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.techplant.affiliate.service.InviteCodeGenerator;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DemoDataInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final List<SeedAccount> accounts;

    public DemoDataInitializer(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            InviteCodeGenerator inviteCodeGenerator,
            @Value("${app.demo.user-a-phone}") String userAPhone,
            @Value("${app.demo.user-a-password}") String userAPassword,
            @Value("${app.demo.user-b-phone}") String userBPhone,
            @Value("${app.demo.user-b-password}") String userBPassword,
            @Value("${app.demo.admin-phone}") String adminPhone,
            @Value("${app.demo.admin-password}") String adminPassword) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.accounts = List.of(
                new SeedAccount(userAPhone, userAPassword, "USER"),
                new SeedAccount(userBPhone, userBPassword, "USER"),
                new SeedAccount(adminPhone, adminPassword, "ADMIN"));
    }

    @Override
    public void run(String... args) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class) > 0) {
            return;
        }
        for (SeedAccount account : accounts) {
            String inviteCode = "USER".equals(account.role()) ? inviteCodeGenerator.generateUniqueCode() : null;
            LocalDateTime now = LocalDateTime.now();
            jdbc.update("""
                    INSERT INTO app_user (phone, password_hash, role, balance, invite_code, created_at, updated_at)
                    VALUES (?, ?, ?, 0.00, ?, ?, ?)
                    """,
                    account.phone(), passwordEncoder.encode(account.password()), account.role(),
                    inviteCode, now, now);
        }
    }

    private record SeedAccount(String phone, String password, String role) {
    }
}
