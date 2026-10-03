package com.techplant.affiliate.service;

import com.techplant.affiliate.common.ApiException;
import com.techplant.affiliate.service.AffiliateService.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RechargeService {
    private static final BigDecimal RATE = new BigDecimal("0.10");
    private final JdbcTemplate jdbc;

    public RechargeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public RechargeResult simulate(long userId, String requestKey, BigDecimal amount) {
        validateRequest(requestKey, amount);

        List<BigDecimal> balances = jdbc.query(
                "SELECT balance FROM app_user WHERE id = ? AND role = 'USER' FOR UPDATE",
                (rs, rowNum) -> rs.getBigDecimal(1), userId);
        if (balances.isEmpty()) throw ApiException.notFound("USER_NOT_FOUND", "用户不存在");

        List<RechargeResult> existing = findByRequest(userId, requestKey);
        if (!existing.isEmpty()) {
            RechargeResult original = existing.get(0);
            if (original.amount().compareTo(amount) != 0) {
                throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "幂等键已用于不同金额");
            }
            return original;
        }

        BigDecimal before = balances.get(0).setScale(2);
        BigDecimal after = before.add(amount).setScale(2);
        List<Long> inviters = jdbc.query(
                "SELECT inviter_user_id FROM affiliate_relation WHERE invitee_user_id = ?",
                (rs, rowNum) -> rs.getLong(1), userId);
        BigDecimal rebate = inviters.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : amount.multiply(RATE).setScale(2, RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();
        String tradeNo = "R" + UUID.randomUUID().toString().replace("-", "");

        jdbc.update("UPDATE app_user SET balance = ?, updated_at = ? WHERE id = ?",
                after, now, userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO recharge_order
                    (out_trade_no, user_id, request_key, amount, rebate_amount, balance_after, paid_at, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, tradeNo);
            ps.setLong(2, userId);
            ps.setString(3, requestKey);
            ps.setBigDecimal(4, amount);
            ps.setBigDecimal(5, rebate);
            ps.setBigDecimal(6, after);
            ps.setObject(7, now);
            ps.setObject(8, now);
            return ps;
        }, keyHolder);
        long orderId = keyHolder.getKey().longValue();

        if (!inviters.isEmpty() && rebate.signum() > 0) {
            jdbc.update("""
                    INSERT INTO affiliate_ledger (user_id, source_order_id, amount, created_at)
                    VALUES (?, ?, ?, ?)
                    """, inviters.get(0), orderId, rebate, now);
        }

        return new RechargeResult(
                Long.toString(orderId), tradeNo, amount, rebate, after, now.toString());
    }

    private List<RechargeResult> findByRequest(long userId, String requestKey) {
        return jdbc.query("""
                SELECT id, out_trade_no, amount, rebate_amount, balance_after, paid_at
                FROM recharge_order
                WHERE user_id = ? AND request_key = ?
                """, (rs, rowNum) -> new RechargeResult(
                Long.toString(rs.getLong("id")),
                rs.getString("out_trade_no"),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("rebate_amount"),
                rs.getBigDecimal("balance_after"),
                rs.getTimestamp("paid_at").toLocalDateTime().toString()),
                userId, requestKey);
    }

    private void validateRequest(String requestKey, BigDecimal amount) {
        if (requestKey == null || requestKey.isBlank() || requestKey.length() > 64) {
            throw ApiException.badRequest("IDEMPOTENCY_KEY_INVALID", "Idempotency-Key 长度必须为 1–64");
        }
        if (amount == null || amount.signum() <= 0) {
            throw ApiException.badRequest("AMOUNT_INVALID", "充值金额必须大于 0");
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() > 2 || normalized.precision() - normalized.scale() > 17) {
            throw ApiException.badRequest("AMOUNT_INVALID", "金额最多两位小数且不能超过数据库精度");
        }
    }

    public record RechargeResult(String orderId, String outTradeNo, BigDecimal amount,
                                 BigDecimal rebateAmount, BigDecimal balanceAfter, String paidAt) {
    }
}
