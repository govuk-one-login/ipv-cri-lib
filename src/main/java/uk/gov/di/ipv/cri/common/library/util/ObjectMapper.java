package uk.gov.di.ipv.cri.common.library.util;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.*;

// Custom ObjectMapper for Java 8 Date/Time support
@SuppressWarnings({"rawtypes", "unchecked"})
public class ObjectMapper extends com.fasterxml.jackson.databind.ObjectMapper {

    // Default Constructor
    public ObjectMapper() {
        this(true);
    }

    public ObjectMapper(boolean registerModules) {
        super();
        if (registerModules) {
            this.registerModules(new Jdk8Module(), new JavaTimeModule());
        }
    }

    public ObjectMapper(com.fasterxml.jackson.databind.ObjectMapper mapper) {
        this(mapper, true);
    }

    // Take an existing standard ObjectMapper and register the modules (if not already registered)
    public ObjectMapper(
            com.fasterxml.jackson.databind.ObjectMapper mapper, boolean registerModules) {
        super(
                mapper.copy() // Create a copy, rather than modifying the existing ObjectMapper
                        .registerModules(
                                registerModules
                                        ? new Module[] {new Jdk8Module(), new JavaTimeModule()}
                                        : new Module[] {})); // Register the modules
    }

    public <T> Map toMap(T object) {
        return toMap(object, new ArrayList<>(), false);
    }

    public <T> Map toMap(T object, List<String> fields, boolean include) {
        var map = super.convertValue(object, Map.class);
        var finalMap = new HashMap();
        Set fieldSet = map.keySet();
        for (var field : fieldSet) {
            if (fields.contains(field.toString()) == include) {
                if (map.get(field) != null) {
                    finalMap.put(field, map.get(field));
                }
            }
        }
        return finalMap;
    }

    public <T> Map toMap(T object, List<String> excludeFields) {
        return toMap(object, excludeFields, false);
    }

    public <T> Map[] toMapArray(List<T> object) {
        return object.stream().map(this::toMap).toArray(Map[]::new);
    }

    public <T> Map[] toMapArray(List<T> object, List<String> fields, boolean include) {
        return object.stream().map(o -> toMap(o, fields, include)).toArray(Map[]::new);
    }

    public <T> Map[] toMapArray(List<T> object, List<String> excludeFields) {
        return toMapArray(object, excludeFields, false);
    }
}
