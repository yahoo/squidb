package com.yahoo.squidb.json;

public interface JSONMapper {

    String toJSON(Object toSerialize);

    <T> T fromJson(String jsonString, Class<?> baseType, Class<?>... genericArgs);
}
