package com.xinghe.emp.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.parser.JsqlParserGlobal;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration(proxyBeanMethods = false)
@MapperScan(basePackages = "com.xinghe.emp.mapper", annotationClass = Mapper.class)
public class MybatisPlusConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService jsqlParserExecutor() {
        // Tomcat 卸载应用时由 Spring 关闭解析线程池。
        var executor = Executors.newFixedThreadPool(2, Thread.ofPlatform().name("ssm-emp-sql-parser-", 1).factory());
        JsqlParserGlobal.setExecutorService(executor);
        return executor;
    }

    @Bean
    @DependsOn("jsqlParserExecutor")
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        var pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(100L);
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    @Bean
    public MybatisSqlSessionFactoryBean sqlSessionFactory(DataSource dataSource, MybatisPlusInterceptor interceptor) {
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);

        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setPlugins(interceptor);
        return factory;
    }
}
