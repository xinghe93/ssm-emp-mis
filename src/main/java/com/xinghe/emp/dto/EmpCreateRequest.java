package com.xinghe.emp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmpCreateRequest(
        String empName,
        Integer gender,
        String dept,
        String post,
        BigDecimal salary,
        LocalDate hireDate,
        Integer status
) {
}
