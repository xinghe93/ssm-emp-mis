package com.xinghe.emp.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinghe.emp.common.BusinessException;
import com.xinghe.emp.dto.EmpCreateRequest;
import com.xinghe.emp.dto.EmpQueryRequest;
import com.xinghe.emp.dto.EmpUpdateRequest;
import com.xinghe.emp.entity.Emp;
import com.xinghe.emp.mapper.EmpMapper;
import com.xinghe.emp.service.EmpService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.xinghe.emp.common.BusinessException.Code.EMPLOYEE_NOT_FOUND;
import static com.xinghe.emp.common.BusinessException.Code.INVALID_ARGUMENT;

@Service
@RequiredArgsConstructor
public class EmpServiceImpl implements EmpService {

    private static final BigDecimal MAX_SALARY = new BigDecimal("9999999999.99");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 100;

    private final EmpMapper mapper;

    @Override
    public Emp create(EmpCreateRequest request) {
        require(request != null, "缺少新增员工信息");
        Integer status = request.status();
        if (status == null) {
            status = 1;
        }
        Emp emp = employee(request.empName(), request.gender(), request.dept(), request.post(),
                request.salary(), request.hireDate(), status);
        mapper.insert(emp);
        return emp;
    }

    @Override
    @Transactional
    public Emp update(EmpUpdateRequest request) {
        require(request != null, "缺少员工更新信息");
        requireValidId(request.empId());
        Emp emp = employee(request.empName(), request.gender(), request.dept(), request.post(),
                request.salary(), request.hireDate(), request.status());
        emp.setEmpId(request.empId());

        mapper.updateById(emp);
        // JDBC 可能只报告实际变更的行数，同内容更新仍应返回当前记录。
        return getById(emp.getEmpId());
    }

    @Override
    public int deleteById(Long id) {
        requireValidId(id);
        int deleted = mapper.deleteById(id);
        if (deleted == 0) {
            throw notFound(id);
        }
        return deleted;
    }

    @Override
    @Transactional
    public int deleteBatch(List<Long> ids) {
        require(ids != null && !ids.isEmpty() && ids.size() <= MAX_BATCH_SIZE, "批量删除需要 1 至 100 个员工 ID");
        ids.forEach(EmpServiceImpl::requireValidId);
        require(new HashSet<>(ids).size() == ids.size(), "员工 ID 不可重复");

        long matched = mapper.selectCount(Wrappers.lambdaQuery(Emp.class).in(Emp::getEmpId, ids));
        if (matched != ids.size()) {
            throw new BusinessException(EMPLOYEE_NOT_FOUND, "批量删除包含不存在的员工");
        }
        int deleted = mapper.deleteByIds(ids);
        if (deleted != ids.size()) {
            throw new BusinessException(EMPLOYEE_NOT_FOUND, "批量删除时部分员工已不存在");
        }
        return deleted;
    }

    @Override
    public Emp getById(Long id) {
        requireValidId(id);
        Emp emp = mapper.selectById(id);
        if (emp == null) {
            throw notFound(id);
        }
        return emp;
    }

    @Override
    public IPage<Emp> findPage(EmpQueryRequest request) {
        require(request != null, "缺少查询条件对象");
        int page = request.page() == null ? 1 : request.page();
        int size = request.size() == null ? DEFAULT_PAGE_SIZE : request.size();
        require(page >= 1, "页码必须大于等于 1");
        require(size >= 1 && size <= MAX_PAGE_SIZE, "每页记录数必须在 1 至 100 之间");
        if (request.gender() != null) {
            requireValidGender(request.gender());
        }

        String name = normalizedText(request.name(), "查询姓名");
        String dept = normalizedText(request.dept(), "查询部门");
        var conditions = Wrappers.lambdaQuery(Emp.class)
                .eq(request.gender() != null, Emp::getGender, request.gender())
                .eq(dept != null, Emp::getDept, dept)
                .orderByDesc(Emp::getEmpId);
        if (name != null) {
            // 保留包含式匹配，将用户输入中的通配符作为普通字符。
            String escaped = name.replace("!", "!!").replace("%", "!%").replace("_", "!_");
            conditions.apply("emp_name LIKE {0} ESCAPE '!'", "%" + escaped + "%");
        }
        return mapper.selectPage(new Page<>(page, size), conditions);
    }

    private static Emp employee(String name, Integer gender, String dept, String post,
                                BigDecimal salary, LocalDate hireDate, Integer status) {
        requireValidGender(gender);
        require(status != null && (status == 0 || status == 1), "任职状态必须为 0 或 1");
        require(salary != null && salary.signum() >= 0 && salary.scale() <= 2 && salary.compareTo(MAX_SALARY) <= 0,
                "薪资必须在 0 至 9999999999.99 之间，且最多保留两位小数");
        require(hireDate != null, "入职日期不能为空");

        var emp = new Emp();
        emp.setEmpName(requiredText(name, "姓名"));
        emp.setGender(gender);
        emp.setDept(requiredText(dept, "部门"));
        emp.setPost(requiredText(post, "岗位"));
        emp.setSalary(salary);
        emp.setHireDate(hireDate);
        emp.setStatus(status);
        return emp;
    }

    private static String requiredText(String value, String field) {
        String normalized = normalizedText(value, field);
        require(normalized != null, field + "不能为空");
        return normalized;
    }

    private static String normalizedText(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        require(normalized.codePointCount(0, normalized.length()) <= 50, field + "不能超过 50 个字符");
        return normalized;
    }

    private static void requireValidGender(Integer gender) {
        require(gender != null && gender >= 0 && gender <= 2, "性别必须为 0、1 或 2");
    }

    private static void requireValidId(Long id) {
        require(id != null && id > 0, "员工 ID 必须为正整数");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(INVALID_ARGUMENT, message);
        }
    }

    private static BusinessException notFound(Long id) {
        return new BusinessException(EMPLOYEE_NOT_FOUND, "员工不存在，ID 为 " + id);
    }
}
