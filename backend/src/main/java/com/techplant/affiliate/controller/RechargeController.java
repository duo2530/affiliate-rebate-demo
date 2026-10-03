package com.techplant.affiliate.controller;

import com.techplant.affiliate.common.ApiResponse;
import com.techplant.affiliate.common.ApiException;
import com.techplant.affiliate.common.SessionAuth;
import com.techplant.affiliate.service.RechargeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recharges")
public class RechargeController {
    private final RechargeService service;
    private final SessionAuth sessionAuth;

    public RechargeController(RechargeService service, SessionAuth sessionAuth) {
        this.service = service;
        this.sessionAuth = sessionAuth;
    }

    @PostMapping("/simulate")
    public ApiResponse<?> simulate(
            @RequestHeader(value = "Idempotency-Key", required = false) String requestKey,
            @Valid @RequestBody SimulateRequest body,
            HttpServletRequest request) {
        RechargeService.RechargeResult result = service.simulate(
                sessionAuth.requireUser(request).id(), requestKey, body.amount());
        Map<String, String> data = new LinkedHashMap<>();
        data.put("orderId", result.orderId());
        data.put("outTradeNo", result.outTradeNo());
        data.put("amount", result.amount().setScale(2, RoundingMode.UNNECESSARY).toPlainString());
        data.put("balanceAfter", result.balanceAfter().setScale(2).toPlainString());
        data.put("paidAt", result.paidAt());
        return ApiResponse.ok(data);
    }

    public record SimulateRequest(@NotNull(message = "充值金额不能为空") BigDecimal amount) {
    }
}
