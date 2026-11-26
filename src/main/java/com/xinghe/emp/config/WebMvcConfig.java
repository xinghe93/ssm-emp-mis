package com.xinghe.emp.config;

import com.xinghe.emp.aspect.RequestLogAspect;
import com.xinghe.emp.common.GlobalExceptionHandler;
import com.xinghe.emp.interceptor.LoginInterceptor;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableWebMvc
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan("com.xinghe.emp.controller")
@Import({GlobalExceptionHandler.class, RequestLogAspect.class})
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final Environment environment;

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        var jsonMapper = JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .build();
        builder.withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String configuredOrigins = environment.getProperty("CORS_ALLOWED_ORIGINS",
                "http://localhost:5173,http://127.0.0.1:5173");
        String[] origins = StringUtils.tokenizeToStringArray(configuredOrigins, ",");
        Assert.isTrue(!Arrays.asList(origins).contains("*"), "CORS_ALLOWED_ORIGINS 必须列出具体来源");

        for (String path : List.of("/auth/**", "/emps", "/emps/**")) {
            registry.addMapping(path)
                    .allowedOrigins(origins)
                    .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT)
                    .exposedHeaders(HttpHeaders.LOCATION)
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor()).addPathPatterns("/emps", "/emps/**");
    }
}
