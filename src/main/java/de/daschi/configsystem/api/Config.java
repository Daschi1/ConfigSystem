package de.daschi.configsystem.api;

import de.daschi.core.MySQL;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.exceptions.InvalidConfigurationException;

import javax.sql.rowset.CachedRowSet;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

public class Config implements ConfigMethods {
    private final ConfigMode configMode;
    private final String folderPath;
    private final String configName;
    private Config cache;

    public Config(final ConfigMode configMode, final String folderPath, final String configName) {
        this(configMode, folderPath, configName, "", -1, "", "", "");
    }

    public Config(final ConfigMode configMode, final String folderPath, final String configName, final String hostname, final int port, final String username, final String password, final String database) {
        this.configMode = configMode;
        this.folderPath = folderPath.endsWith("/") ? folderPath : folderPath + "/";
        this.configName = configName.endsWith(".yml") ? configName : configName + ".yml";

        if (this.configMode.equals(ConfigMode.MYSQL)) {
            this.setupMySQL(hostname, port, username, password, database);
        }
    }

    public ConfigMode getConfigMode() {
        return this.configMode;
    }

    public String getFolderPath() {
        return this.folderPath;
    }

    public Config getCache() {
        return this.cache;
    }

    public String getConfigName() {
        return this.configName;
    }

    void setupMySQL(final String hostname, final int port, final String username, final String password, final String database) {
        this.cache = new Config(ConfigMode.YAML, this.folderPath + "cache/", "cached" + this.getConfigName());
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

    String getValue(final String key) throws IOException, InvalidConfigurationException, SQLException {
        if (this.configMode.equals(ConfigMode.YAML)) {
            final YamlFile yamlFile = new YamlFile(new File(this.folderPath + this.configName));
            if (this.containsValue(key)) {
                yamlFile.loadWithComments();
                return yamlFile.getString(key);
            }
        } else {
            if (this.cache.containsValue(key)) {
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
        return "";
    }

    void setValue(final String key, final String value) throws IOException, InvalidConfigurationException {
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

    boolean containsValue(final String key) throws IOException, InvalidConfigurationException, SQLException {
        if (this.configMode.equals(ConfigMode.YAML)) {
            final YamlFile yamlFile = new YamlFile(new File(this.folderPath + this.configName));
            yamlFile.loadWithComments();
            return yamlFile.contains(key);
        } else {
            final CachedRowSet cachedRowSet = MySQL.query("SELECT * FROM `" + MySQL.preventSQLInjection(this.configName) + "` WHERE `key` = '" + MySQL.preventSQLInjection(key) + "';");
            return cachedRowSet.next();
        }
    }

    void removeValue(final String key) throws IOException, InvalidConfigurationException {
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

    @Override
    public boolean getBoolean(final String key) {
        try {
            return Boolean.parseBoolean(this.getValue(key));
        } catch (final IOException | InvalidConfigurationException | SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public byte getByte(final String key) {
        try {
            return Byte.parseByte(this.getValue(key));
        } catch (final IOException | InvalidConfigurationException | SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public short getShort(final String key) {
        try {
            return Short.parseShort(this.getValue(key));
        } catch (final IOException | InvalidConfigurationException | SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int getInt(final String key) {
        try {
            return Integer.parseInt(this.getValue(key));
        } catch (final IOException | InvalidConfigurationException | SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public long getLong(final String key) {
        try {
            return Long.parseLong(this.getValue(key));
        } catch (final IOException | InvalidConfigurationException | SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public float getFloat(final String key) {
        try {
            return Float.parseFloat(this.getValue(key));
        } catch (final IOException | InvalidConfigurationException | SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public double getDouble(final String key) {
        try {
            return Double.parseDouble(this.getValue(key));
        } catch (final IOException | InvalidConfigurationException | SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public char getChar(final String key) {
        try {
            return this.getValue(key).charAt(0);
        } catch (final IOException | InvalidConfigurationException | SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public String getString(final String key) {
        try {
            return this.getValue(key);
        } catch (final IOException | InvalidConfigurationException | SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public UUID getUUID(final String key) {
        try {
            return UUID.fromString(this.getValue(key));
        } catch (final IOException | InvalidConfigurationException | SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setBoolean(final String key, final boolean value) {
        try {
            this.setValue(key, String.valueOf(value));
        } catch (final IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setByte(final String key, final byte value) {
        try {
            this.setValue(key, String.valueOf(value));
        } catch (final IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setShort(final String key, final short value) {
        try {
            this.setValue(key, String.valueOf(value));
        } catch (final IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setInt(final String key, final int value) {
        try {
            this.setValue(key, String.valueOf(value));
        } catch (final IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setLong(final String key, final long value) {
        try {
            this.setValue(key, String.valueOf(value));
        } catch (final IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setFloat(final String key, final float value) {
        try {
            this.setValue(key, String.valueOf(value));
        } catch (final IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setDouble(final String key, final double value) {
        try {
            this.setValue(key, String.valueOf(value));
        } catch (final IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setChar(final String key, final char value) {
        try {
            this.setValue(key, String.valueOf(value));
        } catch (final IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setString(final String key, final String value) {
        try {
            this.setValue(key, value);
        } catch (final IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUUID(final String key, final UUID value) {
        try {
            this.setValue(key, value.toString());
        } catch (final IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contains(final String key) {
        try {
            this.containsValue(key);
        } catch (final IOException | InvalidConfigurationException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(final String key) {
        try {
            this.removeValue(key);
        } catch (final IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clearCache() {
        final File cacheFolder = new File(this.cache.getFolderPath());
        for (final String s : Objects.requireNonNull(cacheFolder.list())) {
            final File currentFile = new File(cacheFolder.getPath(), s);
            if (!currentFile.delete()) {
                currentFile.deleteOnExit();
            }
        }
    }
}
