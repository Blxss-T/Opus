package com.opsflow.common.tenancy;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class TenantDataSourceConfig implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof HikariDataSource hikari && !(bean instanceof TenantAwareDataSource)) {
            return new TenantAwareDataSource(hikari);
        }
        if (bean instanceof DataSource dataSource
            && !(bean instanceof TenantAwareDataSource)
            && !(bean instanceof HikariDataSource)) {
            return new TenantAwareDataSource(dataSource);
        }
        return bean;
    }
}
