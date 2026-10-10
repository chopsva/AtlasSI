package com.icr.atlas.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ibm.OS4690.FileInputStream4690;

public class INIFile {
    private final Map<String, Map<String, String>> sections = new LinkedHashMap<>();

    public static INIFile load(String path) throws IOException {
    	System.out.println("INIFile:"+path);
        INIFile ini = new INIFile();

        String currentSection = "";
        ini.sections.put(currentSection, new LinkedHashMap<String, String>());
        System.out.println("INIFile TRY");
        //try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream4690(path),StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith(";") || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1).trim();

                    if (!ini.sections.containsKey(currentSection)) {
                        ini.sections.put(currentSection, new LinkedHashMap<String, String>());
                    }

                    continue;
                }

                int equalsIndex = line.indexOf('=');

                if (equalsIndex < 0) {
                    continue;
                }

                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();

                ini.sections.get(currentSection).put(key, value);
            }
        } catch (Exception e) {
        	System.out.println("ERROR "+e);
        	e.printStackTrace();
        }

        return ini;
    }

    public String get(String section, String key) {
        Map<String, String> values = sections.get(section);

        if (values == null) {
            return null;
        }

        return values.get(key);
    }

    public String get(String section, String key, String defaultValue) {
        String value = get(section, key);
        return value == null ? defaultValue : value;
    }

    public int getInt(String section, String key, int defaultValue) {
        String value = get(section, key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String section, String key, boolean defaultValue) {
        String value = get(section, key);

        if (value == null) {
            return defaultValue;
        }

        return value.equalsIgnoreCase("true")
            || value.equalsIgnoreCase("yes")
            || value.equalsIgnoreCase("1")
            || value.equalsIgnoreCase("on");
    }

    public Map<String, Map<String, String>> getSections() {
        return sections;
    }
}