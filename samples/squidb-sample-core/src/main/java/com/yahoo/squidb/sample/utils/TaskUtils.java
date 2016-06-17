/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.utils;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sample.database.TasksDatabase;
import com.yahoo.squidb.sample.models.Tag;
import com.yahoo.squidb.sample.models.Task;
import com.yahoo.squidb.sql.Criterion;
import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Property.StringProperty;
import com.yahoo.squidb.sql.Query;

public class TaskUtils {

    private static TaskUtils sInstance = null;

    public static TaskUtils getInstance() {
        if (sInstance == null) {
            synchronized (TaskUtils.class) {
                if (sInstance == null) {
                    sInstance = new TaskUtils();
                }
            }
        }
        return sInstance;
    }

    private TasksDatabase mTasksDatabase = TasksDatabase.getInstance();

    private TaskUtils() {
    }

    public static final StringProperty TAGS_CONCAT = StringProperty.fromFunction(
            Function.groupConcat(Tag.TAG, " | "), "tags_concat");

    private static final Query TASKS_WITH_TAGS = Query.select(Task.PROPERTIES).selectMore(TAGS_CONCAT)
            .from(Task.TABLE)
            .leftJoin(Tag.TABLE, Task.ID.eq(Tag.TASK_ID))
            .groupBy(Task.ID)
            .freeze();

    public Query getTasksWithTagsQuery(Criterion filterBy) {
        if (filterBy == null) {
            return TASKS_WITH_TAGS;
        }
        // Since the query is frozen, this will create a clone with the given filter
        return TASKS_WITH_TAGS.where(filterBy);
    }

    public Query getOrderedTasksWithTags() {
        Function<Long> unixNow = Function.multiply(1000, Function.functionWithArguments("strftime", "%s", "now"));
        Function<Long> sinceCompletion = Function.subtract(unixNow, Task.COMPLETION_DATE);
        return getTasksWithTagsQuery(Task.COMPLETION_DATE.eq(0)
                .or(sinceCompletion.lt(60000 * 5)))
                .orderBy(Function.caseWhen(Task.DUE_DATE.neq(0)).desc(), Task.DUE_DATE.asc());
    }

    public SquidCursor<Task> getTasksCursor() {
        return mTasksDatabase.query(Task.class, getOrderedTasksWithTags());
    }

    public boolean insertNewTask(String title, int priority, long dueDate, String commaSeparatedTags) {
        String[] tags = commaSeparatedTags.split("\\s*,\\s*");
        mTasksDatabase.beginTransaction();
        try {
            Task task = new Task()
                    .setTitle(title)
                    .setPriority(priority)
                    .setDueDate(dueDate);
            if (mTasksDatabase.persist(task)) {
                for (String tag : tags) {
                    if (tag != null && !tag.isEmpty()) {
                        if (!mTasksDatabase.persist(new Tag().setTag(tag).setTaskId(task.getId()))) {
                            return false;
                        }
                    }
                }
                mTasksDatabase.setTransactionSuccessful();
                return true;
            }
        } finally {
            mTasksDatabase.endTransaction();
        }
        return false;
    }

    public boolean completeTask(Task task) {
        task.setCompletionDate(System.currentTimeMillis());
        return mTasksDatabase.persist(task);
    }

    public boolean deleteTask(Task task) {
        return mTasksDatabase.delete(Task.class, task.getId());
    }

}
