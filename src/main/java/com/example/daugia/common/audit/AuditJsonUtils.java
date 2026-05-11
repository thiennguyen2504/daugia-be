package com.example.daugia.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AuditJsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AuditJsonUtils() {
    }

    public static String toJson(Object... keyValuePairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            values.put(String.valueOf(keyValuePairs[i]), keyValuePairs[i + 1]);
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return values.toString();
        }
    }
}