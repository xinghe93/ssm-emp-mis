package com.xinghe.emp.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmpUpdateRequest(
        @NotNull(message = "不能为空") @Positive(message = "必须为正整数") Long empId,
        @NotBlank(message = "不能为空") String empName,
        @NotNull(message = "不能为空") @Min(value = 0, message = "必须为 0、1 或 2") @Max(value = 2, message = "必须为 0、1 或 2") Integer gender,
        @NotBlank(message = "不能为空") String dept,
        @NotBlank(message = "不能为空") String post,
        @NotNull(message = "不能为空") @PositiveOrZero(message = "不能为负数")
        @Digits(integer = 10, fraction = 2, message = "最多 10 位整数和 2 位小数") BigDecimal salary,
        @NotNull(message = "不能为空") @PastOrPresent(message = "不能晚于当天") LocalDate hireDate,
        @NotNull(message = "不能为空") @Min(value = 0, message = "必须为 0 或 1") @Max(value = 1, message = "必须为 0 或 1") Integer status
) {
}
