package com.xinghe.emp.transaction;

import com.xinghe.emp.config.MybatisPlusConfig;
import com.xinghe.emp.config.TransactionConfig;
import com.xinghe.emp.dto.EmpCreateRequest;
import com.xinghe.emp.service.EmpService;
import com.xinghe.emp.service.impl.EmpServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionBehaviorTest {

    private static AnnotationConfigApplicationContext context;
    private static TransactionProbe probe;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        probe = context.getBean(TransactionProbe.class);
        jdbc = new JdbcTemplate(context.getBean(DataSource.class));
    }

    @AfterAll
    static void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void writeWithoutTransactionRemainsAfterException() {
        String name = "无事务";
        assertThrows(AfterInsertException.class, () -> probe.saveWithoutTransaction(name));
        assertEquals(1L, countEmployees(name));
    }

    @Test
    void writeRollsBackWhenExceptionEscapesTransactionalMethod() {
        String name = "事务回滚";
        assertThrows(AfterInsertException.class, () -> probe.saveWithTransaction(name));
        assertEquals(0L, countEmployees(name));
    }

    @Test
    void catchingExceptionInsideTransactionAllowsCommit() {
        String name = "捕获异常";
        probe.saveAndCatchException(name);
        assertEquals(1L, countEmployees(name));
    }

    @Test
    void selfInvocationBypassesTransactionalProxy() {
        String name = "同类自调用";
        assertThrows(AfterInsertException.class, () -> probe.callOwnTransactionalMethod(name));
        assertEquals(1L, countEmployees(name));
    }

    @Test
    void transactionalAnnotationOnPrivateMethodDoesNotStartTransaction() {
        String name = "私有事务方法";
        assertThrows(AfterInsertException.class, () -> probe.callPrivateMethod(name));
        assertEquals(1L, countEmployees(name));
    }

    private static long countEmployees(String name) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM emp WHERE emp_name = ?", Long.class, name);
    }

    @Configuration(proxyBeanMethods = false)
    @Import({TransactionConfig.class, MybatisPlusConfig.class, EmpServiceImpl.class})
    static class TestConfig {

        @Bean(destroyMethod = "shutdown")
        EmbeddedDatabase dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .addScript("transaction/employee-schema.sql")
                    .build();
        }

        @Bean
        TransactionProbe transactionProbe(EmpService service) {
            return new TransactionProbe(service);
        }
    }

    static class TransactionProbe {

        private final EmpService service;

        TransactionProbe(EmpService service) {
            this.service = service;
        }

        public void saveWithoutTransaction(String name) {
            insertThenFail(name);
        }

        @Transactional
        public void saveWithTransaction(String name) {
            insertThenFail(name);
        }

        @Transactional
        public void saveAndCatchException(String name) {
            try {
                insertThenFail(name);
            } catch (AfterInsertException ignored) {
                // 正常返回时，事务代理没有收到需要回滚的异常。
            }
        }

        public void callOwnTransactionalMethod(String name) {
            saveWithTransaction(name);
        }

        public void callPrivateMethod(String name) {
            saveWithPrivateAnnotation(name);
        }

        @Transactional
        private void saveWithPrivateAnnotation(String name) {
            insertThenFail(name);
        }

        private void insertThenFail(String name) {
            service.create(new EmpCreateRequest(name, 1, "测试部", "测试员",
                    new BigDecimal("100.00"), LocalDate.of(2024, 1, 1), 1));
            throw new AfterInsertException();
        }
    }

    private static class AfterInsertException extends RuntimeException {
    }
}
