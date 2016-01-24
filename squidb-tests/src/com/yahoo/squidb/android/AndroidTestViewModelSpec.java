/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.yahoo.squidb.annotations.ViewModelSpec;
import com.yahoo.squidb.annotations.ViewQuery;
import com.yahoo.squidb.json.JSONProperty;
import com.yahoo.squidb.sql.Query;

import java.util.List;
import java.util.Map;

@ViewModelSpec(className = "AndroidTestViewModel", viewName = "androidTestView", isSubquery = true)
public class AndroidTestViewModelSpec {

    @ViewQuery
    public static final Query QUERY = Query.select().from(AndroidTestModel.TABLE);

    public static final JSONProperty<JSONPojo> JSON_PROP = AndroidTestModel.SOME_POJO;

    public static final JSONProperty<Map<String, Map<String, List<Integer>>>> CRAZY_MAP
            = AndroidTestModel.COMPLICATED_MAP;
}
