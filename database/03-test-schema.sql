-- 先执行 01-schema.sql；只复制结构，不复制业务库中的员工数据。
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS ssm_emp_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE ssm_emp_test.emp LIKE ssm_emp.emp;
