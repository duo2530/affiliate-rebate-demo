package com.techplant.affiliate.controller;

import com.techplant.affiliate.common.ApiResponse;
import com.techplant.affiliate.common.PageData;
import com.techplant.affiliate.common.SessionAuth;
import com.techplant.affiliate.service.AffiliateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
public class AffiliateController {
    private final AffiliateService service;
    private final SessionAuth sessionAuth;

    public AffiliateController(AffiliateService service, SessionAuth sessionAuth) {
        this.service = service;
        this.sessionAuth = sessionAuth;
    }

    @GetMapping("/affiliate")
    public ApiResponse<?> overview(HttpServletRequest request) {
        return ApiResponse.ok(service.overview(sessionAuth.requireUser(request).id()));
    }

    @GetMapping("/invitees")
    public ApiResponse<PageData<AffiliateService.Invitee>> invitees(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.ok(service.invitees(sessionAuth.requireUser(request).id(), page, pageSize));
    }

    @GetMapping("/rebates")
    public ApiResponse<PageData<AffiliateService.Rebate>> rebates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.ok(service.rebates(sessionAuth.requireUser(request).id(), page, pageSize));
    }

    @GetMapping("/recharges")
    public ApiResponse<PageData<AffiliateService.RechargeRow>> recharges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.ok(service.recharges(sessionAuth.requireUser(request).id(), page, pageSize));
    }
}
