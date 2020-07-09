package de.daschi.configsystem.api;

import org.simpleyaml.exceptions.InvalidConfigurationException;

import java.io.IOException;
import java.sql.SQLException;

public class Config {
    private static ConfigHandler configHandler;

    public static ConfigHandler getConfigHandler() {
        return Config.configHandler;
    }

    public static void setConfigHandler(final ConfigHandler configHandler) {
        Config.configHandler = configHandler;
    }

    public static String get(final String key) {
        try {
            return Config.configHandler.getValue(key);
        } catch (final SQLException | InvalidConfigurationException | IOException | NullPointerException exceptions) {
            exceptions.printStackTrace();
        }
        return "An error occurred.";
    }
}
