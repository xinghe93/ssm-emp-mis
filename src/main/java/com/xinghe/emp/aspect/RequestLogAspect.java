package com.xinghe.emp.aspect;

import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

@Aspect
public class RequestLogAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogAspect.class);

    @Around("within(com.xinghe.emp.controller..*) && @within(org.springframework.web.bind.annotation.RestController)")
    public Object logControllerRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return joinPoint.proceed();
        }
        var request = attributes.getRequest();
        Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            int status = result instanceof ResponseEntity<?> response ? response.getStatusCode().value() : 200;
            LOGGER.info("{} {} HTTP {} {}ms", request.getMethod(), route, status, elapsed(start));
            return result;
        } catch (Throwable failure) {
            LOGGER.warn("{} {} 异常 {} {}ms", request.getMethod(), route, failure.getClass().getSimpleName(), elapsed(start));
            throw failure;
        }
    }

    private static long elapsed(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }
}
