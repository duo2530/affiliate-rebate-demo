package com.techplant.affiliate.service;

import com.techplant.affiliate.common.ApiException;
import com.techplant.affiliate.common.PageData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AffiliateService {
    private final JdbcTemplate jdbc;
    private final InviteCodeGenerator inviteCodeGenerator;

    public AffiliateService(JdbcTemplate jdbc, InviteCodeGenerator inviteCodeGenerator) {
        this.jdbc = jdbc;
        this.inviteCodeGenerator = inviteCodeGenerator;
    }

    @Transactional
    public RegisteredUser register(String phone, String passwordHash, String rawInviteCode) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE phone = ?", Integer.class, phone) > 0) {
            throw ApiException.conflict("PHONE_ALREADY_EXISTS", "手机号已注册");
        }

        Long inviterId = null;
        String inviteCode = rawInviteCode == null ? null : rawInviteCode.trim().toUpperCase(Locale.ROOT);
        if (inviteCode != null && !inviteCode.isBlank()) {
            List<Long> ids = jdbc.query(
                    "SELECT id FROM app_user WHERE invite_code = ? AND role = 'USER'",
                    (rs, rowNum) -> rs.getLong(1), inviteCode);
            if (ids.isEmpty()) {
                throw ApiException.badRequest("INVITE_CODE_INVALID", "邀请码无效");
            }
            inviterId = ids.get(0);
        }

        String ownCode = inviteCodeGenerator.generateUniqueCode();
        LocalDateTime now = LocalDateTime.now();
        org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection -> {
            java.sql.PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO app_user (phone, password_hash, role, balance, invite_code, created_at, updated_at)
                    VALUES (?, ?, 'USER', 0.00, ?, ?, ?)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, phone);
            statement.setString(2, passwordHash);
            statement.setString(3, ownCode);
            statement.setObject(4, now);
            statement.setObject(5, now);
            return statement;
        }, keyHolder);
        Long userId = keyHolder.getKey().longValue();

        if (inviterId != null) {
            jdbc.update("""
                    INSERT INTO affiliate_relation (invitee_user_id, inviter_user_id, invite_code_snapshot, bound_at)
                    VALUES (?, ?, ?, ?)
                    """, userId, inviterId, inviteCode, now);
        }
        return new RegisteredUser(userId, phone, "USER", ownCode);
    }

    @Transactional(readOnly = true)
    public Account findByPhone(String phone) {
        List<Account> rows = jdbc.query("""
                SELECT id, phone, password_hash, role, invite_code
                FROM app_user WHERE phone = ?
                """, (rs, rowNum) -> new Account(
                rs.getLong("id"),
                rs.getString("phone"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getString("invite_code")), phone);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Transactional(readOnly = true)
    public Account findById(long userId) {
        List<Account> rows = jdbc.query("""
                SELECT id, phone, password_hash, role, invite_code
                FROM app_user WHERE id = ?
                """, (rs, rowNum) -> new Account(
                rs.getLong("id"),
                rs.getString("phone"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getString("invite_code")), userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Transactional(readOnly = true)
    public Overview overview(long userId) {
        Account account = requireAccount(userId);
        Integer invited = jdbc.queryForObject(
                "SELECT COUNT(*) FROM affiliate_relation WHERE inviter_user_id = ?",
                Integer.class, userId);
        BigDecimal rebate = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0.00) FROM affiliate_ledger WHERE user_id = ?",
                BigDecimal.class, userId);
        BigDecimal balance = jdbc.queryForObject(
                "SELECT balance FROM app_user WHERE id = ?", BigDecimal.class, userId);
        return new Overview(userId, mask(account.phone()), account.inviteCode(),
                money(balance), invited == null ? 0 : invited, money(rebate));
    }

    @Transactional(readOnly = true)
    public PageData<Invitee> invitees(long inviterId, int page, int pageSize) {
        Page pageData = normalizePage(page, pageSize);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM affiliate_relation WHERE inviter_user_id = ?",
                Long.class, inviterId);
        List<Invitee> list = jdbc.query("""
                SELECT u.id, u.phone, r.bound_at
                FROM affiliate_relation r
                JOIN app_user u ON u.id = r.invitee_user_id
                WHERE r.inviter_user_id = ?
                ORDER BY r.bound_at DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> new Invitee(
                Long.toString(rs.getLong("id")), mask(rs.getString("phone")),
                rs.getTimestamp("bound_at").toInstant().toString()),
                inviterId, pageData.size(), pageData.offset());
        return new PageData<>(pageData.page(), pageData.size(), total == null ? 0 : total, list);
    }

    @Transactional(readOnly = true)
    public PageData<Rebate> rebates(long userId, int page, int pageSize) {
        Page pageData = normalizePage(page, pageSize);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM affiliate_ledger WHERE user_id = ?", Long.class, userId);
        List<Rebate> list = jdbc.query("""
                SELECT l.id, invitee.phone AS invitee_phone, o.out_trade_no, l.amount, l.created_at
                FROM affiliate_ledger l
                JOIN recharge_order o ON o.id = l.source_order_id
                JOIN app_user invitee ON invitee.id = o.user_id
                WHERE l.user_id = ?
                ORDER BY l.created_at DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> new Rebate(
                Long.toString(rs.getLong("id")), mask(rs.getString("invitee_phone")),
                rs.getString("out_trade_no"), money(rs.getBigDecimal("amount")),
                rs.getTimestamp("created_at").toInstant().toString()),
                userId, pageData.size(), pageData.offset());
        return new PageData<>(pageData.page(), pageData.size(), total == null ? 0 : total, list);
    }

    @Transactional(readOnly = true)
    public PageData<RechargeRow> recharges(long userId, int page, int pageSize) {
        Page pageData = normalizePage(page, pageSize);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM recharge_order WHERE user_id = ?", Long.class, userId);
        List<RechargeRow> list = jdbc.query("""
                SELECT id, out_trade_no, amount, balance_after, paid_at
                FROM recharge_order WHERE user_id = ?
                ORDER BY paid_at DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> new RechargeRow(
                Long.toString(rs.getLong("id")), rs.getString("out_trade_no"),
                money(rs.getBigDecimal("amount")), money(rs.getBigDecimal("balance_after")),
                rs.getTimestamp("paid_at").toInstant().toString()),
                userId, pageData.size(), pageData.offset());
        return new PageData<>(pageData.page(), pageData.size(), total == null ? 0 : total, list);
    }

    public static Page normalizePage(int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(100, size));
        return new Page(safePage, safeSize, (long) (safePage - 1) * safeSize);
    }

    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2).toPlainString();
    }

    private Account requireAccount(long id) {
        Account account = findById(id);
        if (account == null) throw ApiException.notFound("USER_NOT_FOUND", "用户不存在");
        return account;
    }

    public record RegisteredUser(long id, String phone, String role, String inviteCode) {
    }
    public record Account(long id, String phone, String passwordHash, String role, String inviteCode) {
    }
    public record Overview(long userId, String phone, String inviteCode, String balance,
                           int invitedCount, String rebateBalance) {
    }
    public record Invitee(String userId, String phone, String boundAt) {
    }
    public record Rebate(String id, String inviteePhone, String sourceOrderNo, String amount, String createdAt) {
    }
    public record RechargeRow(String id, String outTradeNo, String amount,
                              String balanceAfter, String paidAt) {
    }
    public record Page(int page, int size, long offset) {
    }

}
