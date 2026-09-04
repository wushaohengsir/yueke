package com.bookmate.controller;

import com.bookmate.common.AuthHelper;
import com.bookmate.common.Result;
import com.bookmate.entity.Contract;
import com.bookmate.service.ContractService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    private final ContractService contractService;
    private final AuthHelper auth;

    public ContractController(ContractService c, AuthHelper auth) {
        this.contractService = c;
        this.auth = auth;
    }

    @GetMapping
    public Result<?> list(@RequestHeader("Authorization") String authHeader) {
        return Result.ok(contractService.listByStudent(auth.userId(authHeader)));
    }

    // 购买课时包（生成待签署合同）
    @PostMapping
    public Result<?> purchase(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, Object> body) {
        long student = auth.userId(authHeader);
        long teacherId = Long.parseLong(String.valueOf(body.get("teacherId")));
        int credits = Integer.parseInt(String.valueOf(body.get("credits")));
        if (credits <= 0 || credits > 200) return Result.fail(400, "课时数须在 1-200 之间");
        Contract c = contractService.purchase(student, teacherId, credits);
        return Result.ok(c);
    }

    // 签署（生效并课时入账）
    @PostMapping("/{id}/sign")
    public Result<?> sign(@RequestHeader("Authorization") String authHeader, @PathVariable long id) {
        boolean ok = contractService.sign(auth.userId(authHeader), id);
        return ok ? Result.ok(true) : Result.fail(400, "签署失败（仅本人待签署合同可签）");
    }
}
