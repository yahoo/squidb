/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.aptutils.model.DeclaredTypeName;

class JSONTypeConstants {

    static final DeclaredTypeName LIST = new DeclaredTypeName("java.util.List");
    static final DeclaredTypeName MAP = new DeclaredTypeName("java.util.Map");

    static final DeclaredTypeName SQUIDB_JSON_SUPPORT = new DeclaredTypeName(
            "com.yahoo.squidb.json.SquidbJSONSupport");

}
