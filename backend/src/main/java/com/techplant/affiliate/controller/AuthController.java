package com.techplant.affiliate.controller;

import com.techplant.affiliate.common.ApiException;
import com.techplant.affiliate.common.ApiResponse;
import com.techplant.affiliate.common.SessionAuth;
import com.techplant.affiliate.service.AffiliateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AffiliateService affiliateService;
    private final PasswordEncoder passwordEncoder;
    private final SessionAuth sessionAuth;

    public AuthController(AffiliateService affiliateService, PasswordEncoder passwordEncoder, SessionAuth sessionAuth) {
        this.affiliateService = affiliateService;
        this.passwordEncoder = passwordEncoder;
        this.sessionAuth = sessionAuth;
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        AffiliateService.RegisteredUser user = affiliateService.register(
                request.phone().trim(), passwordEncoder.encode(request.password()), request.inviteCode());
        SessionAuth.CurrentUser current = new SessionAuth.CurrentUser(user.id(), user.role(), user.phone());
        sessionAuth.establish(servletRequest, current);
        return ApiResponse.ok(Map.of(
                "id", Long.toString(user.id()),
                "phone", AffiliateService.mask(user.phone()),
                "role", user.role(),
                "inviteCode", user.inviteCode()));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        AffiliateService.Account account = affiliateService.findByPhone(request.phone().trim());
        if (account == null || !passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw ApiException.unauthorized("LOGIN_FAILED", "手机号或密码错误");
        }
        HttpSession oldSession = servletRequest.getSession(false);
        if (oldSession != null) oldSession.invalidate();
        sessionAuth.establish(servletRequest, new SessionAuth.CurrentUser(
                account.id(), account.role(), account.phone()));
        return ApiResponse.ok(userView(account));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<?> me(HttpServletRequest request) {
        SessionAuth.CurrentUser user = sessionAuth.current(request);
        AffiliateService.Account account = affiliateService.findById(user.id());
        if (account == null) throw ApiException.notFound("USER_NOT_FOUND", "用户不存在");
        return ApiResponse.ok(userView(account));
    }

    private Map<String, String> userView(AffiliateService.Account account) {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        result.put("id", Long.toString(account.id()));
        result.put("phone", AffiliateService.mask(account.phone()));
        result.put("role", account.role());
        if (account.inviteCode() != null) result.put("inviteCode", account.inviteCode());
        return result;
    }

    public record RegisterRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "1\\d{10}", message = "请输入 11 位手机号")
            String phone,
            @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 64, message = "密码长度须为 8–64 位")
            String password,
            @Size(max = 32, message = "邀请码长度不能超过 32 位")
            String inviteCode) {
    }

    public record LoginRequest(
            @NotBlank(message = "手机号不能为空") String phone,
            @NotBlank(message = "密码不能为空") String password) {
    }
}
