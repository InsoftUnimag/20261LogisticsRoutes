package com.logistics.routes.infrastructure.adapter.out.messaging;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonNodeMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convierte cualquier objeto Java a JsonNode
     */
    public static <T> JsonNode toJsonNode(T object) {
        try {
            return objectMapper.valueToTree(object);
        } catch (Exception e) {
            throw new RuntimeException("Error al convertir objeto a JsonNode", e);
        }
    }

    /**
     * Convierte un JsonNode a cualquier clase Java
     */
    public static <T> T fromJsonNode(JsonNode jsonNode, Class<T> targetClass) {
        try {
            return objectMapper.treeToValue(jsonNode, targetClass);
        } catch (Exception e) {
            throw new RuntimeException("Error al convertir JsonNode a " + targetClass.getSimpleName(), e);
        }
    }

    /**
     * Convierte un JsonNode a un objeto con manejo de excepciones seguro
     */
    public static <T> T fromJsonNode(JsonNode jsonNode, Class<T> targetClass, T defaultValue) {
        try {
            return objectMapper.treeToValue(jsonNode, targetClass);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Combina dos JsonNode (útil para merges)
     */
    public static JsonNode merge(JsonNode source, JsonNode target) {
        ObjectNode result = objectMapper.createObjectNode();
        try {
            objectMapper.readerForUpdating(result).readValue(source);
            objectMapper.readerForUpdating(result).readValue(target);
        } catch (IOException e) {
            throw new RuntimeException("Error al combinar JsonNode", e);
        }
        return result;
    }

    /**
     * Obtiene el ObjectMapper si necesitas operaciones custom
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}