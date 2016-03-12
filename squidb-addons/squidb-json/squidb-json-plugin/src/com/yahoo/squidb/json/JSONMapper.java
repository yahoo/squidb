/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import java.lang.reflect.Type;

/**
 * Interface for clients to provide a library-agnostic JSON serialization engine to the squidb-json plugin. Clients
 * can define implementations of this interface (e.g. as seen in the JSONPropertyTest test cases) and initialize the
 * plugin using {@link JSONPropertySupport#setJSONMapper(JSONMapper)}
 */
public interface JSONMapper {

    String toJSON(Object toSerialize, Type javaType) throws Exception;

    <T> T fromJSON(String jsonString, Type javaType) throws Exception;
}
