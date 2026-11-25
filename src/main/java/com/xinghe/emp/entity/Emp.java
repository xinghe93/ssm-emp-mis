package com.xinghe.emp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("emp")
public class Emp {

    @TableId(value = "emp_id", type = IdType.AUTO)
    private Long empId;
    private String empName;
    private Integer gender;
    private String dept;
    private String post;
    private BigDecimal salary;
    private LocalDate hireDate;
    private Integer status;
}
