/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.jackson;

import com.yahoo.aptutils.model.DeclaredTypeName;

class JacksonTypeConstants {

    static final DeclaredTypeName LIST = new DeclaredTypeName("java.util.List");
    static final DeclaredTypeName ARRAY_LIST = new DeclaredTypeName("java.util.ArrayList");
    static final DeclaredTypeName MAP = new DeclaredTypeName("java.util.Map");
    static final DeclaredTypeName HASH_MAP = new DeclaredTypeName("java.util.HashMap");

    static final DeclaredTypeName COLLECTION_TYPE = new DeclaredTypeName(
            "org.codehaus.jackson.map.type.CollectionType");
    static final DeclaredTypeName MAP_TYPE = new DeclaredTypeName("org.codehaus.jackson.map.type.MapType");

    static final DeclaredTypeName SQUIDB_JACKSON_SUPPORT = new DeclaredTypeName(
            "com.yahoo.squidb.jackson.SquidbJacksonSupport");

}
