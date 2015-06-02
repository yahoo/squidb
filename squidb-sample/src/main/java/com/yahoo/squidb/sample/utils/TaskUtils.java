/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.utils;

import com.yahoo.squidb.sample.models.Tag;
import com.yahoo.squidb.sample.models.Task;
import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.Query;

public class TaskUtils {

    public static final StringProperty TAGS_CONCAT = StringProperty.fromFunction(
            Function.groupConcat(Tag.TAG, " | "), "tags_concat");

    public static final Query TASKS_WITH_TAGS = Query.select(Task.PROPERTIES).selectMore(TAGS_CONCAT)
            .from(Task.TABLE)
            .leftJoin(Tag.TABLE, Task.ID.eq(Tag.TASK_ID))
            .groupBy(Task.ID)
            .freeze();

}
