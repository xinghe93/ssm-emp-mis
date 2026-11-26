package com.xinghe.emp.controller;

import com.xinghe.emp.common.BusinessException;
import com.xinghe.emp.common.PageResult;
import com.xinghe.emp.common.Result;
import com.xinghe.emp.dto.EmpCreateRequest;
import com.xinghe.emp.dto.EmpQueryRequest;
import com.xinghe.emp.dto.EmpUpdateRequest;
import com.xinghe.emp.entity.Emp;
import com.xinghe.emp.service.EmpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import static com.xinghe.emp.common.BusinessException.Code.INVALID_ARGUMENT;

@RestController
@RequestMapping(path = "/emps", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EmpController {

    private final EmpService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<Emp>> create(@Valid @RequestBody EmpCreateRequest body, HttpServletRequest request) {
        Emp employee = service.create(body);
        URI location = URI.create(request.getContextPath() + "/emps/" + employee.getEmpId());
        return ResponseEntity.created(location).body(Result.success(employee));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Emp> update(@Valid @RequestBody EmpUpdateRequest body) {
        return Result.success(service.update(body));
    }

    @DeleteMapping("/{id}")
    public Result<Integer> deleteById(@PathVariable("id") @Positive(message = "员工 ID 必须为正整数") Long id) {
        return Result.success(service.deleteById(id));
    }

    @DeleteMapping(params = "!ids")
    public Result<Integer> deleteBatch(
            @RequestBody
            @Size(min = 1, max = 100, message = "批量删除需要 1 至 100 个员工 ID")
            List<JsonNode> ids) {
        return Result.success(service.deleteBatch(ids.stream().map(EmpController::parseEmployeeId).toList()));
    }

    @GetMapping("/{id}")
    public Result<Emp> getById(@PathVariable("id") @Positive(message = "员工 ID 必须为正整数") Long id) {
        return Result.success(service.getById(id));
    }

    @GetMapping
    public Result<PageResult<Emp>> findPage(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "gender", required = false)
            @Min(value = 0, message = "性别必须为 0、1 或 2") @Max(value = 2, message = "性别必须为 0、1 或 2") Integer gender,
            @RequestParam(name = "dept", required = false) String dept,
            @RequestParam(name = "page", required = false) @Positive(message = "页码必须大于等于 1") Integer page,
            @RequestParam(name = "size", required = false) @Positive(message = "每页记录数必须大于等于 1")
            @Max(value = 100, message = "每页记录数不能超过 100") Integer size) {
        var query = new EmpQueryRequest(name, gender, dept, page, size);
        return Result.success(PageResult.from(service.findPage(query)));
    }

    private static long parseEmployeeId(JsonNode id) {
        if (id == null || !id.isIntegralNumber() || !id.canConvertToLong() || id.longValue() <= 0) {
            throw new BusinessException(INVALID_ARGUMENT, "员工 ID 必须为有效的正整数");
        }
        return id.longValue();
    }
}
