/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ParameterizedTypeBuilder {

    public static ParameterizedType build(final Type rawType, final Type... typeArgs) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return typeArgs;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }

            @Override
            public Type getRawType() {
                return rawType;
            }
        };
    }
}
