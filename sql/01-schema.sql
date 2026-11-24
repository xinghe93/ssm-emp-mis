-- MySQL 8.4；已有 emp 表时停止，不覆盖或自动迁移现有表。
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS ssm_emp
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE ssm_emp;

CREATE TABLE emp (
    emp_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '员工 ID',
    emp_name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender TINYINT NOT NULL COMMENT '性别：0 未说明，1 男，2 女',
    dept VARCHAR(50) NOT NULL COMMENT '部门',
    post VARCHAR(50) NOT NULL COMMENT '岗位',
    salary DECIMAL(12, 2) NOT NULL COMMENT '薪资',
    hire_date DATE NOT NULL COMMENT '入职日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '任职状态：1 在职，0 离职',
    PRIMARY KEY (emp_id),
    INDEX idx_emp_dept (dept),
    CONSTRAINT ck_emp_gender CHECK (gender IN (0, 1, 2)),
    CONSTRAINT ck_emp_salary CHECK (salary >= 0),
    CONSTRAINT ck_emp_status CHECK (status IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '员工信息';
