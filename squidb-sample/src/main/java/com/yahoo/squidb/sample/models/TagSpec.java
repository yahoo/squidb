/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.models;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "Tag", tableName = "tags",
        tableConstraint = "FOREIGN KEY(taskId) references tasks(_id) ON DELETE CASCADE")
public class TagSpec {

    @ColumnSpec(constraints = "NOT NULL")
    String tag;

    @ColumnSpec(constraints = "NOT NULL")
    long taskId;

}
