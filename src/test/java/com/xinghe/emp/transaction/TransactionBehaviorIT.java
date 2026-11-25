package com.xinghe.emp.transaction;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinghe.emp.common.BusinessException;
import com.xinghe.emp.config.RootConfig;
import com.xinghe.emp.dto.EmpCreateRequest;
import com.xinghe.emp.entity.Emp;
import com.xinghe.emp.mapper.EmpMapper;
import com.xinghe.emp.service.EmpService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionBehaviorIT {

    private static AnnotationConfigApplicationContext context;
    private static EmpService service;
    private static EmpMapper mapper;
    private static TransactionProbe probe;
    private static final BatchCountOverride BATCH_COUNT = new BatchCountOverride();
    private final String dept = "事务测试-" + UUID.randomUUID();

    @BeforeAll
    static void openTestDatabase() throws Exception {
        var environment = new StandardEnvironment();
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("transactionTestDatabase", Map.of(
                "DB_URL", environment.getRequiredProperty("TEST_DB_URL"),
                "DB_USERNAME", environment.getRequiredProperty("TEST_DB_USERNAME"),
                "DB_PASSWORD", environment.getRequiredProperty("TEST_DB_PASSWORD")
        )));
        context.register(RootConfig.class, TransactionProbe.class);
        context.refresh();
        try (var connection = context.getBean(DataSource.class).getConnection()) {
            assertEquals("ssm_emp_test", connection.getCatalog(), "事务写入只允许使用测试库");
        }
        service = context.getBean(EmpService.class);
        mapper = context.getBean(EmpMapper.class);
        probe = context.getBean(TransactionProbe.class);
        context.getBean(SqlSessionFactory.class).getConfiguration().addInterceptor(BATCH_COUNT);
    }

    @AfterEach
    void cleanOwnFixtures() {
        mapper.delete(Wrappers.lambdaQuery(Emp.class).eq(Emp::getDept, dept));
    }

    @AfterAll
    static void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void batchDeletionCommitsOnSuccess() {
        var ids = batchFixtures();
        assertEquals(2, service.deleteBatch(ids));
        assertEquals(0, rowCount());
    }

    @Test
    void batchDeletionRollsBackWhenReportedSizeDiffersAfterSqlWrites() {
        var ids = batchFixtures();
        BATCH_COUNT.active.set(true);
        try {
            var exception = assertThrows(BusinessException.class, () -> service.deleteBatch(ids));
            assertEquals(BusinessException.Code.EMPLOYEE_NOT_FOUND, exception.getCode());
        } finally {
            BATCH_COUNT.active.remove();
        }
        assertEquals(2, rowCount(), "真实 DELETE 执行过，退出代理后记录应已回滚");
    }

    @Test
    void runtimeExceptionRollsBackWhenItEscapesProxy() {
        assertThrows(IllegalStateException.class, () -> probe.failRuntime(dept));
        assertEquals(0, rowCount());
    }

    @Test
    void checkedExceptionCommitsByDefault() {
        assertThrows(CheckedFailure.class, () -> probe.failChecked(dept));
        assertEquals(1, rowCount());
    }

    @Test
    void rollbackForRollsBackCheckedException() {
        assertThrows(CheckedFailure.class, () -> probe.failCheckedWithRollback(dept));
        assertEquals(0, rowCount());
    }

    @Test
    void selfInvocationDoesNotEnterTransactionalProxy() {
        assertThrows(IllegalStateException.class, () -> probe.selfInvoke(dept));
        assertEquals(1, rowCount());
    }

    @Test
    void caughtExceptionDoesNotRollBackWhenMethodReturnsNormally() {
        probe.catchFailure(dept);
        assertEquals(1, rowCount());
    }

    private List<Long> batchFixtures() {
        return List.of(service.create(employee(dept)).getEmpId(), service.create(employee(dept)).getEmpId());
    }

    private long rowCount() {
        return mapper.selectCount(Wrappers.lambdaQuery(Emp.class).eq(Emp::getDept, dept));
    }

    private static EmpCreateRequest employee(String dept) {
        return new EmpCreateRequest("事务测试员工", 1, dept, "测试", new BigDecimal("100.00"), LocalDate.of(2024, 1, 1), 1);
    }

    public static class TransactionProbe {
        private final EmpService service;

        public TransactionProbe(EmpService service) {
            this.service = service;
        }

        @Transactional
        public void failRuntime(String dept) {
            service.create(employee(dept));
            throw new IllegalStateException("模拟写入后异常");
        }

        @Transactional
        public void failChecked(String dept) throws CheckedFailure {
            service.create(employee(dept));
            throw new CheckedFailure();
        }

        @Transactional(rollbackFor = CheckedFailure.class)
        public void failCheckedWithRollback(String dept) throws CheckedFailure {
            failChecked(dept);
        }

        public void selfInvoke(String dept) {
            failRuntime(dept);
        }

        @Transactional
        public void catchFailure(String dept) {
            try {
                failRuntime(dept);
            } catch (IllegalStateException ignored) {
                // 演示业务代码吞掉异常后，事务代理看到正常返回。
            }
        }
    }

    public static class CheckedFailure extends Exception {
    }

    @Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
    static class BatchCountOverride implements Interceptor {
        private final ThreadLocal<Boolean> active = new ThreadLocal<>();

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            Object actualCount = invocation.proceed();
            var statement = (MappedStatement) invocation.getArgs()[0];
            if (Boolean.TRUE.equals(active.get()) && statement.getId().equals(EmpMapper.class.getName() + ".deleteByIds")) {
                assertEquals(2, actualCount);
                return 1;
            }
            return actualCount;
        }
    }
}
