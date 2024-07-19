package org.nashtech.zap.config;

import java.io.InputStream;
import java.util.Properties;

public class ZapConfig {
    private Properties properties;

    public ZapConfig() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("zap-config.properties")) {
            if (input == null) {
                throw new RuntimeException("Sorry, unable to find zap-config.properties");
            }
            properties.load(input);
        } catch (Exception ex) {
            throw new RuntimeException("Error loading configuration", ex);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
