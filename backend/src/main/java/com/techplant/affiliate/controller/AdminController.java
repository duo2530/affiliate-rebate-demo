package com.techplant.affiliate.controller;

import com.techplant.affiliate.common.ApiResponse;
import com.techplant.affiliate.common.PageData;
import com.techplant.affiliate.common.SessionAuth;
import com.techplant.affiliate.service.AffiliateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final JdbcTemplate jdbc;
    private final SessionAuth sessionAuth;

    public AdminController(JdbcTemplate jdbc, SessionAuth sessionAuth) {
        this.jdbc = jdbc;
        this.sessionAuth = sessionAuth;
    }

    @GetMapping("/users")
    public ApiResponse<PageData<UserRow>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        sessionAuth.requireAdmin(request);
        AffiliateService.Page p = AffiliateService.normalizePage(page, pageSize);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE role = 'USER'", Long.class);
        List<UserRow> rows = jdbc.query("""
                SELECT id, phone, role, balance, invite_code, created_at
                FROM app_user WHERE role = 'USER'
                ORDER BY created_at DESC LIMIT ? OFFSET ?
                """, (rs, n) -> new UserRow(Long.toString(rs.getLong("id")),
                AffiliateService.mask(rs.getString("phone")), rs.getString("role"),
                AffiliateService.money(rs.getBigDecimal("balance")), rs.getString("invite_code"),
                rs.getTimestamp("created_at").toInstant().toString()), p.size(), p.offset());
        return ApiResponse.ok(new PageData<>(p.page(), p.size(), total == null ? 0 : total, rows));
    }

    @GetMapping("/referral-relations")
    public ApiResponse<PageData<RelationRow>> relations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        sessionAuth.requireAdmin(request);
        AffiliateService.Page p = AffiliateService.normalizePage(page, pageSize);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM affiliate_relation", Long.class);
        List<RelationRow> rows = jdbc.query("""
                SELECT inviter.id inviter_id, inviter.phone inviter_phone,
                       invitee.id invitee_id, invitee.phone invitee_phone,
                       r.invite_code_snapshot, r.bound_at
                FROM affiliate_relation r
                JOIN app_user inviter ON inviter.id = r.inviter_user_id
                JOIN app_user invitee ON invitee.id = r.invitee_user_id
                ORDER BY r.bound_at DESC LIMIT ? OFFSET ?
                """, (rs, n) -> new RelationRow(Long.toString(rs.getLong("inviter_id")),
                AffiliateService.mask(rs.getString("inviter_phone")),
                Long.toString(rs.getLong("invitee_id")),
                AffiliateService.mask(rs.getString("invitee_phone")),
                rs.getString("invite_code_snapshot"), rs.getTimestamp("bound_at").toInstant().toString()),
                p.size(), p.offset());
        return ApiResponse.ok(new PageData<>(p.page(), p.size(), total == null ? 0 : total, rows));
    }

    @GetMapping("/recharge-orders")
    public ApiResponse<PageData<RechargeRow>> recharges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        sessionAuth.requireAdmin(request);
        AffiliateService.Page p = AffiliateService.normalizePage(page, pageSize);
        String search = keyword == null ? "" : keyword.trim();
        Long total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM recharge_order o JOIN app_user u ON u.id = o.user_id
                WHERE (? = '' OR u.phone LIKE ? OR o.out_trade_no LIKE ?)
                """, Long.class, search, "%" + search + "%", "%" + search + "%");
        List<RechargeRow> rows = jdbc.query("""
                SELECT o.id, o.out_trade_no, o.user_id, u.phone, o.amount,
                       o.rebate_amount, o.paid_at
                FROM recharge_order o JOIN app_user u ON u.id = o.user_id
                WHERE (? = '' OR u.phone LIKE ? OR o.out_trade_no LIKE ?)
                ORDER BY o.paid_at DESC LIMIT ? OFFSET ?
                """, (rs, n) -> new RechargeRow(Long.toString(rs.getLong("id")),
                rs.getString("out_trade_no"), Long.toString(rs.getLong("user_id")),
                AffiliateService.mask(rs.getString("phone")),
                AffiliateService.money(rs.getBigDecimal("amount")),
                AffiliateService.money(rs.getBigDecimal("rebate_amount")),
                rs.getTimestamp("paid_at").toInstant().toString()),
                search, "%" + search + "%", "%" + search + "%", p.size(), p.offset());
        return ApiResponse.ok(new PageData<>(p.page(), p.size(), total == null ? 0 : total, rows));
    }

    @GetMapping("/rebate-ledgers")
    public ApiResponse<PageData<RebateRow>> rebates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        sessionAuth.requireAdmin(request);
        AffiliateService.Page p = AffiliateService.normalizePage(page, pageSize);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM affiliate_ledger", Long.class);
        List<RebateRow> rows = jdbc.query("""
                SELECT l.id, l.user_id, owner.phone owner_phone, o.out_trade_no,
                       invitee.phone invitee_phone, l.amount, l.created_at
                FROM affiliate_ledger l
                JOIN app_user owner ON owner.id = l.user_id
                JOIN recharge_order o ON o.id = l.source_order_id
                JOIN app_user invitee ON invitee.id = o.user_id
                ORDER BY l.created_at DESC LIMIT ? OFFSET ?
                """, (rs, n) -> new RebateRow(Long.toString(rs.getLong("id")),
                Long.toString(rs.getLong("user_id")),
                AffiliateService.mask(rs.getString("owner_phone")),
                rs.getString("out_trade_no"), AffiliateService.mask(rs.getString("invitee_phone")),
                AffiliateService.money(rs.getBigDecimal("amount")),
                rs.getTimestamp("created_at").toInstant().toString()), p.size(), p.offset());
        return ApiResponse.ok(new PageData<>(p.page(), p.size(), total == null ? 0 : total, rows));
    }

    public record UserRow(String id, String phone, String role, String balance,
                          String inviteCode, String createdAt) {
    }
    public record RelationRow(String inviterUserId, String inviterPhone, String inviteeUserId,
                              String inviteePhone, String inviteCode, String boundAt) {
    }
    public record RechargeRow(String id, String outTradeNo, String userId, String phone,
                              String amount, String rebateAmount, String paidAt) {
    }
    public record RebateRow(String id, String userId, String ownerPhone, String sourceOrderNo,
                            String inviteePhone, String amount, String createdAt) {
    }
}
