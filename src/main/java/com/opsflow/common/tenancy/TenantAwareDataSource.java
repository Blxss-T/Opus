package com.opsflow.common.tenancy;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Applies RLS GUCs on every borrowed connection and clears them on close so
 * Hikari cannot leak tenant context between requests.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    public TenantAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return wrap(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return wrap(super.getConnection(username, password));
    }

    private Connection wrap(Connection connection) throws SQLException {
        applySettings(connection);
        InvocationHandler handler = (proxy, method, args) -> {
            if ("close".equals(method.getName()) && (args == null || args.length == 0)) {
                try {
                    clearSettings(connection);
                } finally {
                    return method.invoke(connection, args);
                }
            }
            return method.invoke(connection, args);
        };
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            handler
        );
    }

    static void applySettings(Connection connection) throws SQLException {
        if (!isPostgres(connection)) {
            return;
        }
        UUID orgId = TenantContext.getOrganizationId();
        setConfig(connection, "app.organization_id", orgId == null ? "" : orgId.toString());
        setConfig(connection, "app.login_email", nullToEmpty(TenantContext.getLoginEmail()));
        setConfig(connection, "app.api_key_hash", nullToEmpty(TenantContext.getApiKeyHash()));
        setConfig(connection, "app.refresh_token_hash", nullToEmpty(TenantContext.getRefreshTokenHash()));
    }

    static void clearSettings(Connection connection) throws SQLException {
        if (!isPostgres(connection)) {
            return;
        }
        setConfig(connection, "app.organization_id", "");
        setConfig(connection, "app.login_email", "");
        setConfig(connection, "app.api_key_hash", "");
        setConfig(connection, "app.refresh_token_hash", "");
    }

    private static void setConfig(Connection connection, String key, String value) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT set_config('" + key + "', '" + escape(value) + "', false)");
        }
    }

    private static boolean isPostgres(Connection connection) throws SQLException {
        String product = connection.getMetaData().getDatabaseProductName();
        return product != null && product.toLowerCase().contains("postgres");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String escape(String value) {
        return value.replace("'", "''");
    }
}
