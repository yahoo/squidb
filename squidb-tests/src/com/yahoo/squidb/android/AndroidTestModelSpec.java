/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.net.Uri;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.jackson.JacksonProperty;

import java.util.List;
import java.util.Map;

@TableModelSpec(className = "AndroidTestModel", tableName = "androidTestModels")
public class AndroidTestModelSpec {

    public static final Uri CONTENT_URI = Uri.parse("content://com.yahoo.squidb/androidTestModels");

    @ColumnSpec(defaultValue = "[]")
    @JacksonProperty
    List<String> someList;

    @JacksonProperty
    Map<String, Integer> someMap;

}
