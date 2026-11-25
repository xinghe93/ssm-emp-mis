package com.xinghe.emp.dto;

public record EmpQueryRequest(String name, Integer gender, String dept, Integer page, Integer size) {
}
