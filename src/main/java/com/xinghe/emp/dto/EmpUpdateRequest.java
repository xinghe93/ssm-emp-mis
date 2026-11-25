package com.xinghe.emp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmpUpdateRequest(
        Long empId,
        String empName,
        Integer gender,
        String dept,
        String post,
        BigDecimal salary,
        LocalDate hireDate,
        Integer status
) {
}
