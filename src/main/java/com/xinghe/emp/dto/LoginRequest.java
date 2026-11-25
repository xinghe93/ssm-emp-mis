package com.xinghe.emp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "不能为空") @Size(max = 128, message = "长度不能超过 128") String username,
        @NotBlank(message = "不能为空") @Size(max = 256, message = "长度不能超过 256") String password
) {
    @Override
    public String toString() {
        return "LoginRequest[credentials redacted]";
    }
}
