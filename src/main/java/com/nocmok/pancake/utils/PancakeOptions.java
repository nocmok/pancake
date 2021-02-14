package com.nocmok.pancake.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PancakeOptions {

    private Map<String, Object> options;

    public PancakeOptions() {
        options = new HashMap<>();
    }

    public PancakeOptions(PancakeOptions other) {
        options = new HashMap<>(other.options);
    }

    public void put(String key, Object value) {
        key = key.toLowerCase();
        options.put(key, value);
    }

    /**
     * 
     * @return true if specified key was not presented, false otherwise
     */
    public boolean putIfNotPresent(String key, Object value) {
        key = key.toLowerCase();
        return options.putIfAbsent(key, value) == null;
    }

    /**
     * Adds specified extra options to this options. Replaces values from
     * intersecting keys with new ones from extra options.
     * 
     * @param extraOptions options to add.
     */
    public void update(PancakeOptions extraOptions) {
        if (extraOptions == null) {
            return;
        }
        this.options.putAll(extraOptions.options);
    }

    public boolean contains(String key) {
        return options.containsKey(key);
    }

    public Object get(String key) {
        key = key.toLowerCase();
        return options.getOrDefault(key, null);
    }

    public Object getOr(String key, Object alternative) {
        return Optional.ofNullable(get(key)).orElse(alternative);
    }

    public Integer getInt(String key) {
        key = key.toLowerCase();
        Object value = options.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Integer getIntOr(String key, Integer alternative) {
        return Optional.ofNullable(getInt(key)).orElse(alternative);
    }

    private Boolean parseBoolean(String value) {
        value = value.toLowerCase();
        try {
            return Boolean.parseBoolean(value.toString());
        } catch (NumberFormatException e) {
            List<String> trueStrings = List.of("yes");
            List<String> falseStrings = List.of("no");
            if (trueStrings.contains(value)) {
                return true;
            }
            if (falseStrings.contains(value)) {
                return false;
            }
            return null;
        }
    }

    public Boolean getBool(String key) {
        key = key.toLowerCase();
        Object value = options.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return parseBoolean(value.toString());
        }
        if (value instanceof Number) {
            Number number = (Number) value;
            if (number.equals(0)) {
                return false;
            }
            if (number.equals(1)) {
                return true;
            }
        }
        return null;
    }

    public Boolean getBoolOr(String key, Boolean alternative) {
        return Optional.ofNullable(getBool(key)).orElse(alternative);
    }

    public Double getDouble(String key) {
        key = key.toLowerCase();
        Object value = options.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public String getString(String key) {
        key = key.toLowerCase();
        Object value = options.get(key);
        return value == null ? null : value.toString();
    }

    public String getStringOr(String key, String alternative) {
        return Optional.ofNullable(getString(key)).orElse(alternative);
    }

    public List<String> getAsGdalOptions() {
        List<String> gdalOptions = new ArrayList<>();
        for (var entry : options.entrySet()) {
            gdalOptions.add(entry.getKey().toString().toUpperCase() + "=" + entry.getValue().toString().toUpperCase());
        }
        return gdalOptions;
    }

    public void clear() {
        options.clear();
    }

    public Set<String> keys() {
        return options.keySet();
    }
}
