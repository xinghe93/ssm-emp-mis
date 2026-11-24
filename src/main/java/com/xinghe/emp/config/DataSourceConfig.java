package com.xinghe.emp.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:application.properties")
public class DataSourceConfig {

    @Bean(initMethod = "init", destroyMethod = "close")
    public DruidDataSource dataSource(Environment environment) {
        String url = requiredSetting(environment, "DB_URL");
        String username = requiredSetting(environment, "DB_USERNAME");
        String password = requiredSetting(environment, "DB_PASSWORD");

        var dataSource = new DruidDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(1);
        dataSource.setMaxActive(environment.getRequiredProperty("emp.datasource.max-active", Integer.class));
        dataSource.setMaxWait(environment.getRequiredProperty("emp.datasource.max-wait-ms", Long.class));
        dataSource.setConnectTimeout(environment.getRequiredProperty("emp.datasource.connect-timeout-ms", Integer.class));
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        dataSource.setInitExceptionThrow(true);
        return dataSource;
    }

    private static String requiredSetting(Environment environment, String key) {
        String value = environment.getProperty(key);
        Assert.hasText(value, "缺少有效的数据源配置 " + key);
        return value;
    }
}
