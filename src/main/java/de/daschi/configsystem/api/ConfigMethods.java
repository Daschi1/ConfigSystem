package de.daschi.configsystem.api;

import java.util.UUID;

public interface ConfigMethods {
    boolean getBoolean(String key);

    byte getByte(String key);

    short getShort(String key);

    int getInt(String key);

    long getLong(String key);

    float getFloat(String key);

    double getDouble(String key);

    char getChar(String key);

    String getString(String key);

    UUID getUUID(String key);

    void setBoolean(String key, boolean value);

    void setByte(String key, byte value);

    void setShort(String key, short value);

    void setInt(String key, int value);

    void setLong(String key, long value);

    void setFloat(String key, float value);

    void setDouble(String key, double value);

    void setChar(String key, char value);

    void setString(String key, String value);

    void setUUID(String key, UUID value);

    void contains(String key);

    void remove(String key);

    void clearCache();
}
