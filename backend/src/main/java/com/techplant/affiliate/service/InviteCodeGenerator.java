package com.techplant.affiliate.service;

import com.techplant.affiliate.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class InviteCodeGenerator {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int MAX_ATTEMPTS = 20;
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public InviteCodeGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM app_user WHERE invite_code = ?",
                    Integer.class, code.toString());
            if (count != null && count == 0) return code.toString();
        }
        throw ApiException.conflict("INVITE_CODE_GENERATION_FAILED", "邀请码生成失败，请重试");
    }
}
