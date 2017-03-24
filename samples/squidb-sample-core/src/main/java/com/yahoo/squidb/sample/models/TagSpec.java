/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.models;

import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.tables.ColumnName;
import com.yahoo.squidb.annotations.tables.constraints.ConstraintSql;
import com.yahoo.squidb.annotations.tables.constraints.NotNull;
import com.yahoo.squidb.annotations.tables.constraints.PrimaryKey;

@TableModelSpec(className = "Tag", tableName = "tags")
@ConstraintSql("FOREIGN KEY(taskId) references tasks(_id) ON DELETE CASCADE")
public class TagSpec {

    @PrimaryKey
    @ColumnName("_id")
    long id;

    @NotNull
    String tag;

    @NotNull
    long taskId;

}
