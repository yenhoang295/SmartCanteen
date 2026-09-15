package com.smartcanteen.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBConnection {

    private static Properties props;

    private static Properties loadProps() {
        if (props != null) return props;
        props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) throw new RuntimeException("Khong tim thay db.properties");
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Loi doc db.properties: " + e.getMessage(), e);
        }
        return props;
    }

    /** Doc bien moi truong truoc, neu khong co thi fallback ve db.properties (chay local) */
    private static String env(String envKey, String propsKey, String defaultVal) {
        String v = System.getenv(envKey);
        if (v != null && !v.isBlank()) return v;
        return loadProps().getProperty(propsKey, defaultVal);
    }

    public static int getServerPort() {
        // Render/Railway... cap phat cong qua bien moi truong PORT
        String port = System.getenv("PORT");
        if (port != null && !port.isBlank()) return Integer.parseInt(port);
        return Integer.parseInt(loadProps().getProperty("server.port", "8080"));
    }

    public static Connection getConnection() throws Exception {
        String host = env("DB_HOST", "db.host", "localhost");
        String port = env("DB_PORT", "db.port", "1433");
        String dbName = env("DB_NAME", "db.name", "SmartCanteen");
        String user = env("DB_USER", "db.user", "sa");
        String pass = env("DB_PASSWORD", "db.password", "");

        String url = "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + dbName
                + ";user=" + user + ";password=" + pass
                + ";encrypt=true;trustServerCertificate=true";

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return DriverManager.getConnection(url);
    }
}
