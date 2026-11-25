package com.xinghe.emp.service;

import com.xinghe.emp.common.BusinessException;
import com.xinghe.emp.config.RootConfig;
import com.xinghe.emp.dto.EmpCreateRequest;
import com.xinghe.emp.dto.EmpQueryRequest;
import com.xinghe.emp.dto.EmpUpdateRequest;
import com.xinghe.emp.entity.Emp;
import com.xinghe.emp.mapper.EmpMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.xinghe.emp.common.BusinessException.Code.EMPLOYEE_NOT_FOUND;
import static com.xinghe.emp.common.BusinessException.Code.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmpServiceIT {

    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 2, 29);
    private static final BigDecimal SALARY = new BigDecimal("12345.67");
    private static AnnotationConfigApplicationContext context;
    private static EmpService service;
    private static EmpMapper mapper;
    private static TransactionTemplate transaction;

    @BeforeAll
    static void connectToTestDatabase() throws Exception {
        var environment = new StandardEnvironment();
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("integrationDatabase", Map.of(
                "DB_URL", environment.getRequiredProperty("TEST_DB_URL"),
                "DB_USERNAME", environment.getRequiredProperty("TEST_DB_USERNAME"),
                "DB_PASSWORD", environment.getRequiredProperty("TEST_DB_PASSWORD")
        )));
        context.register(RootConfig.class);
        context.refresh();
        var dataSource = context.getBean(DataSource.class);
        try (var connection = dataSource.getConnection()) {
            assertEquals("ssm_emp_test", connection.getCatalog(), "员工写入测试只能使用 ssm_emp_test");
        }
        mapper = context.getBean(EmpMapper.class);
        service = context.getBean(EmpService.class);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @AfterAll
    static void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void createsReadsUpdatesAndPhysicallyDeletesAnEmployee() {
        inRollbackTransaction(() -> {
            String dept = testDepartment();
            Emp created = service.create(new EmpCreateRequest(" 张小明 ", 1, " " + dept, " 工程师 ", SALARY, HIRE_DATE, null));
            assertNotNull(created.getEmpId());
            assertTrue(created.getEmpId() > 0);

            Emp stored = service.getById(created.getEmpId());
            assertEquals("张小明", stored.getEmpName());
            assertEquals(dept, stored.getDept());
            assertEquals("工程师", stored.getPost());
            assertEquals(SALARY, stored.getSalary());
            assertEquals(HIRE_DATE, stored.getHireDate());
            assertEquals(1, stored.getStatus());

            var changes = new EmpUpdateRequest(created.getEmpId(), "张小明", 0, dept, "主管", new BigDecimal("15000.50"), HIRE_DATE, 0);
            assertEquals(0, service.update(changes).getStatus());
            assertEquals("主管", service.update(changes).getPost());
            assertEquals(new BigDecimal("15000.50"), service.getById(created.getEmpId()).getSalary());

            assertEquals(1, service.deleteById(created.getEmpId()));
            assertNull(mapper.selectById(created.getEmpId()));
            assertEquals(EMPLOYEE_NOT_FOUND, assertThrows(BusinessException.class, () -> service.getById(created.getEmpId())).getCode());
            assertEquals(EMPLOYEE_NOT_FOUND, assertThrows(BusinessException.class, () -> service.update(changes)).getCode());
            assertEquals(EMPLOYEE_NOT_FOUND, assertThrows(BusinessException.class, () -> service.deleteById(created.getEmpId())).getCode());
        });
    }

    @Test
    void batchDeletionRejectsMissingAndDuplicateIdsBeforeRemovingRows() {
        inRollbackTransaction(() -> {
            String dept = testDepartment();
            Long first = create("批量甲", dept, 1, 1).getEmpId();
            Long second = create("批量乙", dept, 2, 0).getEmpId();
            Long removed = create("已删除", dept, 0, 1).getEmpId();
            service.deleteById(removed);

            assertEquals(EMPLOYEE_NOT_FOUND, assertThrows(BusinessException.class, () -> service.deleteBatch(List.of(first, removed))).getCode());
            assertNotNull(mapper.selectById(first));
            assertEquals(INVALID_ARGUMENT, assertThrows(BusinessException.class, () -> service.deleteBatch(List.of(first, first))).getCode());
            assertEquals(INVALID_ARGUMENT, assertThrows(BusinessException.class, () -> service.deleteBatch(List.of())).getCode());
            assertEquals(2, service.deleteBatch(List.of(first, second)));
            assertTrue(mapper.selectByIds(List.of(first, second)).isEmpty());
        });
    }

    @Test
    void combinesFiltersAndReturnsStablePagesIncludingDepartedEmployees() {
        inRollbackTransaction(() -> {
            String dept = testDepartment();
            Long older = create("甲星河", dept, 1, 1).getEmpId();
            create("乙星河", dept, 2, 1);
            Long newer = create("丙星河", dept, 1, 0).getEmpId();
            create("不匹配姓名", dept, 1, 1);
            create("丁星河", testDepartment(), 1, 1);
            Long literal = create("甲_100%!", dept, 2, 1).getEmpId();
            create("甲A100XY!", dept, 2, 1);

            var first = service.findPage(new EmpQueryRequest(" 星河 ", 1, dept, 1, 1));
            assertEquals(2, first.getTotal());
            assertEquals(List.of(newer), first.getRecords().stream().map(Emp::getEmpId).toList());
            assertEquals(0, first.getRecords().getFirst().getStatus());

            var second = service.findPage(new EmpQueryRequest("星河", 1, dept, 2, 1));
            assertEquals(List.of(older), second.getRecords().stream().map(Emp::getEmpId).toList());
            var beyondLast = service.findPage(new EmpQueryRequest("星河", 1, dept, 3, 1));
            assertEquals(2, beyondLast.getTotal());
            assertTrue(beyondLast.getRecords().isEmpty());

            var defaults = service.findPage(new EmpQueryRequest(" ", null, dept, null, null));
            assertEquals(1, defaults.getCurrent());
            assertEquals(10, defaults.getSize());
            assertEquals(6, defaults.getTotal());
            var special = service.findPage(new EmpQueryRequest("_100%!", 2, dept, 1, 10));
            assertEquals(List.of(literal), special.getRecords().stream().map(Emp::getEmpId).toList());
        });
    }

    @Test
    void rejectsInvalidRequestsAtTheServiceBoundary() {
        inRollbackTransaction(() -> {
            String dept = testDepartment();
            var roundedSalary = new EmpCreateRequest("员工", 1, dept, "工程师", new BigDecimal("12.345"), HIRE_DATE, 1);
            assertEquals(INVALID_ARGUMENT, assertThrows(BusinessException.class, () -> service.create(roundedSalary)).getCode());
            var invalidStatus = new EmpCreateRequest("员工", 1, dept, "工程师", SALARY, HIRE_DATE, 2);
            assertEquals(INVALID_ARGUMENT, assertThrows(BusinessException.class, () -> service.create(invalidStatus)).getCode());
            assertEquals(INVALID_ARGUMENT, assertThrows(BusinessException.class, () -> service.getById(0L)).getCode());
            assertEquals(INVALID_ARGUMENT, assertThrows(BusinessException.class, () -> service.findPage(new EmpQueryRequest(null, null, dept, 0, 10))).getCode());
            assertEquals(INVALID_ARGUMENT, assertThrows(BusinessException.class, () -> service.findPage(new EmpQueryRequest(null, 1, dept, 1, 101))).getCode());
            assertEquals(0, service.findPage(new EmpQueryRequest(null, null, dept, null, null)).getTotal());
        });
    }

    private static Emp create(String name, String dept, int gender, int status) {
        return service.create(new EmpCreateRequest(name, gender, dept, "工程师", SALARY, HIRE_DATE, status));
    }

    private static String testDepartment() {
        return "测试-" + UUID.randomUUID();
    }

    private static void inRollbackTransaction(Runnable assertions) {
        transaction.executeWithoutResult(status -> {
            status.setRollbackOnly();
            assertions.run();
        });
    }
}
