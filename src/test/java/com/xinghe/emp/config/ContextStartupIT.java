package com.xinghe.emp.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.extension.parser.JsqlParserGlobal;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinghe.emp.mapper.PaginationProbeMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextStartupIT {

    @Test
    void parentAndMvcContextStartWithMySqlAndExecutePagination() throws Exception {
        var environment = new StandardEnvironment();
        var servletContext = new MockServletContext();
        ExecutorService parserExecutor;
        try (var root = new AnnotationConfigWebApplicationContext();
             var mvc = new AnnotationConfigWebApplicationContext()) {
            root.setServletContext(servletContext);
            root.getEnvironment().getPropertySources().addFirst(new MapPropertySource("integrationDatabase", Map.of(
                    "DB_URL", environment.getRequiredProperty("TEST_DB_URL"),
                    "DB_USERNAME", environment.getRequiredProperty("TEST_DB_USERNAME"),
                    "DB_PASSWORD", environment.getRequiredProperty("TEST_DB_PASSWORD")
            )));
            root.register(RootConfig.class);
            root.refresh();
            parserExecutor = root.getBean("jsqlParserExecutor", ExecutorService.class);
            assertSame(parserExecutor, JsqlParserGlobal.getExecutorService());

            mvc.setParent(root);
            mvc.setServletContext(servletContext);
            mvc.register(WebMvcConfig.class);
            mvc.refresh();

            assertEquals(1, root.getBeansOfType(DataSource.class).size());
            assertEquals(1, root.getBeansOfType(SqlSessionFactory.class).size());
            assertTrue(mvc.getBeansOfType(DataSource.class).isEmpty());
            var dataSource = root.getBean(DruidDataSource.class);
            assertSame(dataSource, mvc.getBean(DataSource.class));
            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT DATABASE(), 1")) {
                assertTrue(result.next());
                assertEquals("ssm_emp_test", result.getString(1));
                assertEquals(1, result.getInt(2));
                assertTrue(connection.getMetaData().getDatabaseProductVersion().startsWith("8."));
            }

            var factory = root.getBean(SqlSessionFactory.class);
            assertSame(dataSource, factory.getConfiguration().getEnvironment().getDataSource());
            assertTrue(factory.getConfiguration().isMapUnderscoreToCamelCase());
            var page = new Page<Integer>(2, 2);
            var rows = root.getBean(PaginationProbeMapper.class).selectValues(page);
            assertEquals(List.of(3), rows);
            assertEquals(3, page.getTotal());
        }
        assertTrue(parserExecutor.isTerminated(), "关闭根容器时应释放 SQL 解析线程");
    }
}
