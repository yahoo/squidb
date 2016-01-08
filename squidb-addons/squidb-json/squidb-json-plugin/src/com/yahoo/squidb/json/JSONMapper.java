package com.yahoo.squidb.json;

public interface JSONMapper {

    String toJSON(Object toSerialize) throws Exception;

    <T> T fromJson(String jsonString, Class<?> baseType, Class<?>... genericArgs) throws Exception;
}
