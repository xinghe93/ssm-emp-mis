package com.xinghe.emp.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({DataSourceConfig.class, MybatisPlusConfig.class, TransactionConfig.class})
@ComponentScan("com.xinghe.emp.service")
public class RootConfig {
}
