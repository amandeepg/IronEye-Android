package com.ironeye;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MiddleServerProperties {

    private static Properties props;

    public static String get(String key) {
        loadProperties();
        return props.getProperty(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBoolean(String key) {
        String val = get(key);
        return "true".equals(val) || "yes".equals(val);
    }

    private static void loadProperties() {
        try {
            props = new Properties();
            InputStream input = new FileInputStream("config.properties");

            props.load(input);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}