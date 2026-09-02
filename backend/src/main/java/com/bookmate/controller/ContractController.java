package com.bookmate.controller;

import com.bookmate.common.Result;
import com.bookmate.entity.Contract;
import com.bookmate.service.ContractService;
import com.bookmate.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    private final ContractService contractService;
    private final JwtUtil jwtUtil;

    public ContractController(ContractService c, JwtUtil j) {
        this.contractService = c;
        this.jwtUtil = j;
    }

    private long currentUserId(String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : auth;
        return jwtUtil.parseUserId(token);
    }

    @GetMapping
    public Result<?> list(@RequestHeader("Authorization") String auth) {
        return Result.ok(contractService.listByStudent(currentUserId(auth)));
    }

    // 购买课时包（生成待签署合同）
    @PostMapping
    public Result<?> purchase(@RequestHeader("Authorization") String auth, @RequestBody Map<String, Object> body) {
        long student = currentUserId(auth);
        long teacherId = Long.parseLong(String.valueOf(body.get("teacherId")));
        int credits = Integer.parseInt(String.valueOf(body.get("credits")));
        if (credits <= 0 || credits > 200) return Result.fail(400, "课时数须在 1-200 之间");
        Contract c = contractService.purchase(student, teacherId, credits);
        return Result.ok(c);
    }

    // 签署（生效并课时入账）
    @PostMapping("/{id}/sign")
    public Result<?> sign(@RequestHeader("Authorization") String auth, @PathVariable long id) {
        boolean ok = contractService.sign(currentUserId(auth), id);
        return ok ? Result.ok(true) : Result.fail(400, "签署失败（仅本人待签署合同可签）");
    }
}
