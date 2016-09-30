/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.utility.SquidUtilities;
import com.yahoo.squidb.utility.VersionCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triggers are database operations that are automatically performed when a specified database event occurs.
 * <p>
 * SQLite only supports FOR EACH ROW triggers. This implies that the operations specified in the trigger may be
 * executed for each database row being inserted, updated or deleted by the statement causing the trigger to fire. If a
 * WHEN clause is supplied, the operations specified are executed once for each row that satisfies the WHEN clause.
 * <p>
 * Both the WHEN clause and the trigger actions may access column values of the row being inserted, deleted, or
 * updated. Use {@link #oldValueOf(Property)} and {@link #newValueOf(Property)} to create references to the column
 * property of the table this trigger is associated with.
 *
 * @see <a href="http://www.sqlite.org/lang_createtrigger.html">http://www.sqlite.org/lang_createtrigger.html</a>
 */
public class Trigger extends DBObject<Trigger> implements SqlStatement {

    private static final Table OLD = new Table(TableModel.class, null, "OLD");
    private static final Table NEW = new Table(TableModel.class, null, "NEW");

    private SqlTable<?> table;
    private TriggerType triggerType;
    private TriggerEvent triggerEvent;
    private boolean isTemp;
    private final List<Property<?>> columns = new ArrayList<>();
    private final List<Criterion> criterions = new ArrayList<>();
    private final List<TableStatement> statements = new ArrayList<>();

    private enum TriggerType {
        BEFORE("BEFORE"), AFTER("AFTER"), INSTEAD("INSTEAD OF");

        final String name;

        TriggerType(String name) {
            this.name = name;
        }
    }

    private enum TriggerEvent {
        DELETE, INSERT, UPDATE
    }

    protected Trigger(String name, TriggerType triggerType) {
        super(name);
        this.triggerType = triggerType;
    }

    /**
     * Construct a Trigger that triggers before an operation on table
     *
     * @param name the name of the Trigger
     * @return a new Trigger instance
     */
    public static Trigger before(String name) {
        return new Trigger(name, TriggerType.BEFORE);
    }

    /**
     * Construct a Trigger that triggers after an operation on table
     *
     * @param name the name of the Trigger
     * @return a new Trigger instance
     */
    public static Trigger after(String name) {
        return new Trigger(name, TriggerType.AFTER);
    }

    /**
     * Construct a Trigger that triggers instead of an operation on table. Note: INSTEAD OF triggers can only be
     * created on Views.
     *
     * @param name the name of the Trigger
     * @return a new Trigger instance
     */
    public static Trigger insteadOf(String name) {
        return new Trigger(name, TriggerType.INSTEAD);
    }

    /**
     * Construct a temporary Trigger that triggers before an operation on table
     *
     * @param name the name of the Trigger
     * @return a new Trigger instance
     */
    public static Trigger tempBefore(String name) {
        Trigger trigger = before(name);
        trigger.isTemp = true;
        return trigger;
    }

    /**
     * Construct a temporary Trigger that triggers after an operation on table
     *
     * @param name the name of the Trigger
     * @return a new Trigger instance
     */
    public static Trigger tempAfter(String name) {
        Trigger trigger = after(name);
        trigger.isTemp = true;
        return trigger;
    }

    /**
     * Construct a temporary Trigger that triggers instead of an operation on table. Note: INSTEAD OF triggers can only
     * be created on Views.
     *
     * @param name the name of the Trigger
     * @return a new Trigger instance
     */
    public static Trigger tempInsteadOf(String name) {
        Trigger trigger = insteadOf(name);
        trigger.isTemp = true;
        return trigger;
    }

    /**
     * Set this trigger to execute when a delete operation occurs on the specified {@link Table}
     *
     * @param table the Table
     * @return this Trigger instance, for chaining method calls
     */
    public Trigger deleteOn(Table table) {
        return deleteOnTable(table);
    }

    /**
     * Set this trigger to execute when a delete operation occurs on the specified {@link View}. This trigger will be
     * changed to an INSTEAD OF trigger if it isn't one already.
     *
     * @param view the View
     * @return this Trigger instance, for chaining method calls
     */
    public Trigger deleteOn(View view) {
        Trigger result = deleteOnTable(view);
        result.triggerType = TriggerType.INSTEAD;
        return result;
    }

    private Trigger deleteOnTable(SqlTable<?> table) {
        assertNoTriggerEvent();
        this.table = table;
        triggerEvent = TriggerEvent.DELETE;
        return this;
    }

    /**
     * Set this trigger to execute when an insert operation occurs on the specified {@link Table}
     *
     * @param table the Table
     * @return this Trigger instance, for chaining method calls
     */
    public Trigger insertOn(Table table) {
        return insertOnTable(table);
    }

    /**
     * Set this trigger to execute when an insert operation occurs on the specified {@link View}. This trigger will be
     * changed to an INSTEAD OF trigger if it isn't one already.
     *
     * @param view the View
     * @return this Trigger instance, for chaining method calls
     */
    public Trigger insertOn(View view) {
        Trigger result = insertOnTable(view);
        result.triggerType = TriggerType.INSTEAD;
        return result;
    }

    private Trigger insertOnTable(SqlTable<?> table) {
        assertNoTriggerEvent();
        this.table = table;
        triggerEvent = TriggerEvent.INSERT;
        return this;
    }

    /**
     * Set this trigger to execute when an update operation occurs on the specified columns of the specified
     * {@link Table}. This trigger will be changed to an INSTEAD OF trigger if it isn't one already. To trigger on any
     * column in the table, pass null as the second argument or omit it entirely.
     *
     * @param table the Table
     * @param columns the columns which activate the trigger
     * @return this Trigger instance, for chaining method calls
     */
    public Trigger updateOn(Table table, Property<?>... columns) {
        return updateOnTable(table, columns);
    }

    /**
     * Set this trigger to execute when an update operation on the specified columns of the specified {@link View}.
     * This trigger will be changed to an INSTEAD OF trigger if it isn't one already. To trigger on any column in the
     * view, pass null as the second argument or omit it entirely.
     *
     * @param view the View
     * @param columns the columns which activate the trigger
     * @return this Trigger instance, for chaining method calls
     */
    public Trigger updateOn(View view, Property<?>... columns) {
        Trigger result = updateOnTable(view, columns);
        result.triggerType = TriggerType.INSTEAD;
        return result;
    }

    private Trigger updateOnTable(SqlTable<?> table, Property<?>... columns) {
        assertNoTriggerEvent();
        this.table = table;
        triggerEvent = TriggerEvent.UPDATE;
        SquidUtilities.addAll(this.columns, columns);
        return this;
    }

    private void assertNoTriggerEvent() {
        if (triggerEvent != null) {
            throw new IllegalStateException("Trigger event already specified for this trigger.");
        }
    }

    /**
     * Set a conditional expression that restricts which rows will cause statements to be performed when the trigger is
     * activated. By default, the statements will execute for every row affected by the statement that activates the
     * trigger.
     *
     * @param criterion the {@link Criterion} to match on
     * @return this Trigger instance, for chaining method calls
     */
    public Trigger when(Criterion criterion) {
        if (criterion != null) {
            criterions.add(criterion);
        }
        return this;
    }

    /**
     * Add statements to be performed when this trigger activates. The statements will execute in the order you pass
     * them to this method.
     *
     * @param statements the statements to perform.
     * @return this Trigger instance, for chaining method calls
     */
    public Trigger perform(TableStatement... statements) {
        Collections.addAll(this.statements, statements);
        return this;
    }

    /**
     * Create a reference to the old value of a column in a row affected by the statement that activates the trigger.
     * References of this kind are valid for triggers that fire on an update or delete.
     *
     * @param property the {@link Property} associated with the column
     * @return a new {@link Property} qualified to reference the old value
     */
    @SuppressWarnings("unchecked")
    public static <T extends Property<?>> T oldValueOf(T property) {
        return (T) property.as(OLD, property.getExpression());
    }

    /**
     * Create a reference to the new value of a column in a row affected by the statement that activates the trigger.
     * References of this kind are valid for triggers that fire on an insert or update. Note that the new value of the
     * rowid is undefined in a BEFORE INSERT trigger in which the rowid is not explicitly set to an integer.
     *
     * @param property the {@link Property} associated with the column
     * @return a new {@link Property} qualified to reference the new value
     */
    @SuppressWarnings("unchecked")
    public static <T extends Property<?>> T newValueOf(T property) {
        return (T) property.as(NEW, property.getExpression());
    }

    @Override
    @Deprecated
    public CompiledStatement compile(VersionCode sqliteVersion) {
        return compile(CompileContext.defaultContextForVersionCode(sqliteVersion));
    }

    public CompiledStatement compile(CompileContext compileContext) {
        // Android's argument binding doesn't handle trigger statements, so we settle for a sanitized sql statement.
        return new CompiledStatement(toRawSql(compileContext), EMPTY_ARGS, false);
    }

    @Override
    void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation) {
        assertTriggerEvent();
        assertStatements();

        visitCreateTrigger(builder.sql);
        visitTriggerType(builder.sql);
        visitTriggerEvent(builder.sql);
        visitWhen(builder, forSqlValidation);
        visitStatements(builder);
    }

    private void assertTriggerEvent() {
        if (triggerEvent == null) {
            throw new IllegalStateException(
                    "No trigger event (ON DELETE, ON INSERT, or ON UPDATE) specified for this trigger.");
        }
    }

    private void assertStatements() {
        if (statements.isEmpty()) {
            throw new IllegalStateException("No statements specified for this trigger.");
        }
    }

    private void visitCreateTrigger(StringBuilder sql) {
        sql.append("CREATE ");
        if (isTemp) {
            sql.append("TEMP ");
        }
        sql.append("TRIGGER IF NOT EXISTS ").append(getExpression()).append(" ");
    }

    private void visitTriggerType(StringBuilder sql) {
        if (triggerType != null) {
            sql.append(triggerType.name).append(" ");
        }
    }

    private void visitTriggerEvent(StringBuilder sql) {
        sql.append(triggerEvent.name());
        if (TriggerEvent.UPDATE == triggerEvent && !columns.isEmpty()) {
            sql.append(" OF ");
            for (Property<?> column : columns) {
                sql.append(column.getExpression()).append(",");
            }
            sql.deleteCharAt(sql.length() - 1);
        }
        sql.append(" ON ").append(table.getExpression()).append(" ");
    }

    private void visitWhen(SqlBuilder builder, boolean forSqlValidation) {
        if (criterions.isEmpty()) {
            return;
        }
        builder.sql.append("WHEN ");
        builder.appendConcatenatedCompilables(criterions, " AND ", forSqlValidation);
        builder.sql.append(" ");
    }

    private void visitStatements(SqlBuilder builder) {
        builder.sql.append("BEGIN ");
        for (int i = 0; i < statements.size(); i++) {
            // Android's argument binding doesn't handle trigger statements, so we settle for a sanitized sql statement.
            builder.sql.append(statements.get(i).toRawSql(builder.compileContext)).append("; ");
        }
        builder.sql.append("END");
    }
}
