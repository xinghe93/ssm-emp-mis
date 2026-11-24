package com.xinghe.emp.config;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JdbcCleanupListener implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcCleanupListener.class);

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        ClassLoader applicationLoader = getClass().getClassLoader();
        if (AbandonedConnectionCleanupThread.class.getClassLoader() == applicationLoader) {
            AbandonedConnectionCleanupThread.checkedShutdown();
        }
        DriverManager.drivers()
                .filter(driver -> driver.getClass().getClassLoader() == applicationLoader)
                .forEach(JdbcCleanupListener::deregister);
    }

    private static void deregister(Driver driver) {
        try {
            DriverManager.deregisterDriver(driver);
        } catch (SQLException e) {
            LOGGER.warn("无法注销 JDBC 驱动 {}", driver.getClass().getName(), e);
        }
    }
}
