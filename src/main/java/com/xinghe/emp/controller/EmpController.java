package com.xinghe.emp.controller;

import com.xinghe.emp.common.PageResult;
import com.xinghe.emp.common.Result;
import com.xinghe.emp.dto.EmpCreateRequest;
import com.xinghe.emp.dto.EmpQueryRequest;
import com.xinghe.emp.dto.EmpUpdateRequest;
import com.xinghe.emp.entity.Emp;
import com.xinghe.emp.service.EmpService;
import jakarta.servlet.http.HttpServletRequest;
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

@RestController
@RequestMapping(path = "/emps", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EmpController {

    private final EmpService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Result<Emp>> create(@RequestBody EmpCreateRequest body, HttpServletRequest request) {
        Emp employee = service.create(body);
        URI location = URI.create(request.getContextPath() + "/emps/" + employee.getEmpId());
        return ResponseEntity.created(location).body(Result.success(employee));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Emp> update(@RequestBody EmpUpdateRequest body) {
        return Result.success(service.update(body));
    }

    @DeleteMapping("/{id}")
    public Result<Integer> deleteById(@PathVariable("id") Long id) {
        return Result.success(service.deleteById(id));
    }

    @DeleteMapping
    public Result<Integer> deleteBatch(@RequestParam("ids") List<Long> ids) {
        return Result.success(service.deleteBatch(ids));
    }

    @GetMapping("/{id}")
    public Result<Emp> getById(@PathVariable("id") Long id) {
        return Result.success(service.getById(id));
    }

    @GetMapping
    public Result<PageResult<Emp>> findPage(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "gender", required = false) Integer gender,
            @RequestParam(name = "dept", required = false) String dept,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        var query = new EmpQueryRequest(name, gender, dept, page, size);
        return Result.success(PageResult.from(service.findPage(query)));
    }
}
