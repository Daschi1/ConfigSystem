package de.daschi.configsystem.api;

import de.daschi.core.MySQL;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.exceptions.InvalidConfigurationException;

import javax.sql.rowset.CachedRowSet;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public class ConfigHandler { // TODO: 09.07.2020 interface and need Config class?
    private final ConfigMode configMode;
    private final String folderPath;
    private ConfigHandler cache;
    private final String configName;

    public ConfigMode getConfigMode() {
        return this.configMode;
    }

    public String getFolderPath() {
        return this.folderPath;
    }

    public ConfigHandler getCache() {
        return this.cache;
    }

    public String getConfigName() {
        return this.configName;
    }

    public ConfigHandler(final ConfigMode configMode, final String folderPath, final String configName) {
        this(configMode, folderPath, configName, "", -1, "", "", "");
    }

    public ConfigHandler(final ConfigMode configMode, final String folderPath, final String configName, final String hostname, final int port, final String username, final String password, final String database) {
        this.configMode = configMode;
        this.folderPath = folderPath.endsWith("/") ? folderPath : folderPath + "/";
        this.configName = configName.endsWith(".yml") ? configName : configName + ".yml";

        if (this.configMode.equals(ConfigMode.MYSQL)) {
            this.setupMySQL(hostname, port, username, password, database);
        }
    }

    private void setupMySQL(final String hostname, final int port, final String username, final String password, final String database) {
        this.cache = new ConfigHandler(ConfigMode.YAML, this.folderPath + "cache/", "cached" + this.getConfigName());
        MySQL.using(new MySQL(hostname, port, username, password, database));
        MySQL.autoDisconnect(true);

        MySQL.update("CREATE TABLE IF NOT EXISTS `" + MySQL.preventSQLInjection(this.configName) + "` " +
                "(" +
                "`key` text," +
                "`value` text," +
                "UNIQUE(`key`)" +
                ");");

        Runtime.getRuntime().addShutdownHook(new Thread(this::clearCache));
    }

    public void clearCache() {
        final File cacheFolder = new File(this.cache.getFolderPath());
        for (final String s : Objects.requireNonNull(cacheFolder.list())) {
            final File currentFile = new File(cacheFolder.getPath(), s);
            if (!currentFile.delete()) {
                currentFile.deleteOnExit();
            }
        }
    }

    public String getValue(final String key) throws SQLException, InvalidConfigurationException, IOException, NullPointerException {
        if (this.configMode.equals(ConfigMode.YAML)) {
            final YamlFile yamlFile = new YamlFile(new File(this.folderPath + this.configName));
            if (this.hasValue(key)) {
                yamlFile.loadWithComments();
                return yamlFile.getString(key);
            }
        } else {
            if (this.cache.hasValue(key)) {
                return this.cache.getValue(key);
            } else {
                final CachedRowSet cachedRowSet = MySQL.query("SELECT * FROM `" + MySQL.preventSQLInjection(this.configName) + "` WHERE `key` = '" + MySQL.preventSQLInjection(key) + "';");
                if (cachedRowSet.next()) {
                    final String value = cachedRowSet.getString("value");
                    this.cache.setValue(key, value);
                    return value;
                }
            }
        }
        throw new NullPointerException("Could not found a value for the key '" + key + "'.");
    }

    public void setValue(final String key, final String value) throws IOException, InvalidConfigurationException {
        if (this.configMode.equals(ConfigMode.YAML)) {
            final File file = new File(this.folderPath + this.configName);
            final YamlFile yamlFile = new YamlFile(file);
            if (!yamlFile.exists()) {
                yamlFile.createNewFile(true);
            }
            yamlFile.loadWithComments();
            yamlFile.set(key, value);
            yamlFile.saveWithComments();
        } else {
            MySQL.update("INSERT INTO `" + MySQL.preventSQLInjection(this.configName) + "` (`key`, `value`) VALUES ('" + MySQL.preventSQLInjection(key) + "', '" + MySQL.preventSQLInjection(value) + "') ON DUPLICATE KEY UPDATE `value` = '" + MySQL.preventSQLInjection(value) + "';");
        }
    }

    public boolean hasValue(final String key) throws InvalidConfigurationException, IOException, SQLException {
        if (this.configMode.equals(ConfigMode.YAML)) {
            final YamlFile yamlFile = new YamlFile(new File(this.folderPath + this.configName));
            yamlFile.loadWithComments();
            return yamlFile.contains(key);
        } else {
            final CachedRowSet cachedRowSet = MySQL.query("SELECT * FROM `" + MySQL.preventSQLInjection(this.configName) + "` WHERE `key` = '" + MySQL.preventSQLInjection(key) + "';");
            return cachedRowSet.next();
        }
    }

    public void removeValue(final String key) throws InvalidConfigurationException, IOException {
        if (this.configMode.equals(ConfigMode.YAML)) {
            final File file = new File(this.folderPath + this.configName);
            final YamlFile yamlFile = new YamlFile(file);
            yamlFile.loadWithComments();
            yamlFile.remove(key);
            yamlFile.saveWithComments();
        } else {
            MySQL.update("DELETE FROM `" + MySQL.preventSQLInjection(this.configName) + "` WHERE `key` = '" + MySQL.preventSQLInjection(key) + "';");
        }
    }
}
