/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.ViewModel;
import com.yahoo.squidb.utility.SquidUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder class for a SQLite SELECT statement
 */
public final class Query extends TableStatement {

    /** Specifies this query has no limit */
    public static final Field<Integer> NO_LIMIT = Field.field("-1");
    /** Specifies this query has no offset */
    public static final Field<Integer> NO_OFFSET = Field.field("0");

    private SqlTable<?> table = null;
    private ArrayList<Field<?>> fields = null;
    private ArrayList<Criterion> criterions = null;
    private ArrayList<Join> joins = null;
    private ArrayList<Field<?>> groupByFields = null;
    private ArrayList<Criterion> havings = null;
    private ArrayList<CompoundSelect> compoundSelects = null;
    private ArrayList<Order> orders = null;
    private Field<Integer> limit = NO_LIMIT;
    private Field<Integer> offset = NO_OFFSET;
    private boolean distinct = false;
    private boolean immutable = false;

    private boolean needsValidation = false;

    private ArrayList<Field<?>> selectAllCache = null;

    private Query(List<Field<?>> fields) {
        if (!isEmpty(fields)) {
            this.fields = new ArrayList<>(fields);
        }
    }

    private Query(Field<?>... fields) {
        if (!isEmpty(fields)) {
            this.fields = new ArrayList<>();
            SquidUtilities.addAll(this.fields, fields);
        }
    }

    /**
     * Construct a new Select statement that queries for the specified {@link Field Fields}
     *
     * @param fields the Fields to select
     * @return a new Query object
     */
    public static Query select(Field<?>... fields) {
        return new Query(fields);
    }

    /**
     * Construct a new Select statement that queries for the specified {@link Field Fields}
     *
     * @param fields the Fields to select
     * @return a new Query object
     */
    public static Query select(List<Field<?>> fields) {
        return new Query(fields);
    }

    /**
     * Construct a new Select statement that queries for distinct values of the specified {@link Field Fields}
     *
     * @param fields the Fields to select
     * @return a new Query object
     */
    public static Query selectDistinct(Field<?>... fields) {
        Query query = new Query(fields);
        query.distinct = true;
        return query;
    }

    /**
     * Construct a new Select statement that queries for distinct values of the specified {@link Field Fields}
     *
     * @param fields the Fields to select
     * @return a new Query object
     */
    public static Query selectDistinct(List<Field<?>> fields) {
        Query query = new Query(fields);
        query.distinct = true;
        return query;
    }

    /**
     * Construct a new Select statement that queries all the {@link Field}s of the given subquery
     *
     * @param subquery the inner Query object
     * @param subqueryAlias the name to alias the subquery
     * @return a new Query object
     */
    public static Query fromSubquery(Query subquery, String subqueryAlias) {
        SubqueryTable table = subquery.as(subqueryAlias);
        return Query.select(table.qualifiedFields()).from(table);
    }

    /**
     * Construct a new Select statement that queries all the {@link Field Fields} of the given {@link View}
     *
     * @param view the View to select from
     * @return a new Query object
     */
    public static Query fromView(View view) {
        return Query.select(view.qualifiedFields()).from(view);
    }

    /**
     * Add more {@link Field Fields} to be selected
     *
     * @param fields the additional Fields to be selected
     * @return this Query object, to allow chaining method calls
     */
    public Query selectMore(Field<?>... fields) {
        if (immutable) {
            return fork().selectMore(fields);
        }
        if (!isEmpty(fields)) {
            if (this.fields == null) {
                this.fields = new ArrayList<>();
            }
            SquidUtilities.addAll(this.fields, fields);
            if (selectAllCache != null) {
                selectAllCache.clear();
            }
            invalidateCompileCache();
        }
        return this;
    }

    /**
     * Add more {@link Field Fields} to be selected
     *
     * @param fields the additional Fields to be selected
     * @return this Query object, to allow chaining method calls
     */
    public Query selectMore(List<Field<?>> fields) {
        if (immutable) {
            return fork().selectMore(fields);
        }
        if (!isEmpty(fields)) {
            if (this.fields == null) {
                this.fields = new ArrayList<>(fields);
            } else {
                this.fields.addAll(fields);
            }
            if (selectAllCache != null) {
                selectAllCache.clear();
            }
            invalidateCompileCache();
        }
        return this;
    }

    /**
     * Set the {@link SqlTable table} this query selects from
     *
     * @param table the table to select from
     * @return this Query object, to allow chaining method calls
     */
    public Query from(SqlTable<?> table) {
        if (immutable) {
            return fork().from(table);
        }
        if (this.table != table) {
            this.table = table;
            if (selectAllCache != null) {
                selectAllCache.clear();
            }
            invalidateCompileCache();
        }
        return this;
    }

    /**
     * Add a {@link Join} to this query
     *
     * @param joins one or more joins to apply to this query
     * @return this Query object, to allow chaining method calls
     */
    public Query join(Join... joins) {
        if (immutable) {
            return fork().join(joins);
        }
        if (this.joins == null) {
            this.joins = new ArrayList<>();
        }
        SquidUtilities.addAll(this.joins, joins);
        if (selectAllCache != null) {
            selectAllCache.clear();
        }
        invalidateCompileCache();
        return this;
    }

    /**
     * Add a left {@link Join} to this query using the ON clause
     *
     * @param table the table to join on
     * @param onCriterions one or more criterions to use for the "on" clause
     * @return this Query object, to allow chaining method calls
     */
    public Query leftJoin(SqlTable<?> table, Criterion... onCriterions) {
        return join(Join.left(table, onCriterions));
    }

    /**
     * Add a left {@link Join} to this query using the USING clause
     *
     * @param table the table to join on
     * @param usingColumns one or more columns to use for the "using" clause
     * @return this Query object, to allow chaining method calls
     */
    public Query leftJoin(SqlTable<?> table, Property<?>... usingColumns) {
        return join(Join.left(table, usingColumns));
    }

    /**
     * Add an inner {@link Join} to this query using the ON clause
     *
     * @param table the table to join on
     * @param onCriterions one or more criterions to use for the "on" clause
     * @return this Query object, to allow chaining method calls
     */
    public Query innerJoin(SqlTable<?> table, Criterion... onCriterions) {
        return join(Join.inner(table, onCriterions));
    }

    /**
     * Add an inner {@link Join} to this query using the USING clause
     *
     * @param table the table to join on
     * @param usingColumns one or more columns to use for the "using" clause
     * @return this Query object, to allow chaining method calls
     */
    public Query innerJoin(SqlTable<?> table, Property<?>... usingColumns) {
        return join(Join.inner(table, usingColumns));
    }

    /**
     * Add a {@link Criterion} to the WHERE clause of this query. Multiple calls will combine all the criterions with
     * AND.
     *
     * @param criterion the Criterion to add to the WHERE clause
     * @return this Query object, to allow chaining method calls
     */
    public Query where(Criterion criterion) {
        if (criterion == null) {
            return this;
        }
        if (immutable) {
            return fork().where(criterion);
        }
        if (criterions == null) {
            criterions = new ArrayList<>();
        }
        criterions.add(criterion);
        invalidateCompileCache();
        return this;
    }

    /**
     * Add a GROUP BY clause (or an additional grouping term) to this query
     *
     * @param fields one or more Fields to group on
     * @return this Query object, to allow chaining method calls
     */
    public Query groupBy(Field<?>... fields) {
        if (immutable) {
            return fork().groupBy(fields);
        }
        if (this.groupByFields == null) {
            this.groupByFields = new ArrayList<>();
        }
        SquidUtilities.addAll(this.groupByFields, fields);
        invalidateCompileCache();
        return this;
    }

    /**
     * Add a {@link Criterion} to the HAVING clause of this query. Multiple calls will combine all the criterions with
     * AND.
     *
     * @param criterion the Criterion to add to the HAVING clause
     * @return this Query object, to allow chaining method calls
     */
    public Query having(Criterion criterion) {
        if (criterion == null) {
            return this;
        }
        if (immutable) {
            return fork().having(criterion);
        }
        if (this.havings == null) {
            this.havings = new ArrayList<>();
        }
        this.havings.add(criterion);
        invalidateCompileCache();
        return this;
    }

    /**
     * Form a compound select with the given query using the UNION operator
     *
     * @param query a Query object to append with the UNION operator
     * @return this Query object, to allow chaining method calls
     * @see <a href="http://www.sqlite.org/lang_select.html#compound">http://www.sqlite.org/lang_select.html#compound</a>
     */
    public Query union(Query query) {
        if (immutable) {
            return fork().union(query);
        }
        addCompoundSelect(CompoundSelect.union(query));
        return this;
    }

    /**
     * Form a compound select with the given query using the UNION ALL operator
     *
     * @param query a Query object to append with the UNION ALL operator
     * @return this Query object, to allow chaining method calls
     * @see <a href="http://www.sqlite.org/lang_select.html#compound">http://www.sqlite.org/lang_select.html#compound</a>
     */
    public Query unionAll(Query query) {
        if (immutable) {
            return fork().unionAll(query);
        }
        addCompoundSelect(CompoundSelect.unionAll(query));
        return this;
    }

    /**
     * Form a compound select with the given query using the INTERSECT operator
     *
     * @param query a Query object to append with the INTERSECT operator
     * @return this Query object, to allow chaining method calls
     * @see <a href="http://www.sqlite.org/lang_select.html#compound">http://www.sqlite.org/lang_select.html#compound</a>
     */
    public Query intersect(Query query) {
        if (immutable) {
            return fork().intersect(query);
        }
        addCompoundSelect(CompoundSelect.intersect(query));
        return this;
    }

    /**
     * Form a compound select with the given query using the EXCEPT operator
     *
     * @param query a Query object to append with the EXCEPT operator
     * @return this Query object, to allow chaining method calls
     * @see <a href="http://www.sqlite.org/lang_select.html#compound">http://www.sqlite.org/lang_select.html#compound</a>
     */
    public Query except(Query query) {
        if (immutable) {
            return fork().except(query);
        }
        addCompoundSelect(CompoundSelect.except(query));
        return this;
    }

    private void addCompoundSelect(CompoundSelect compoundSelect) {
        if (this.compoundSelects == null) {
            this.compoundSelects = new ArrayList<>();
        }
        this.compoundSelects.add(compoundSelect);
        invalidateCompileCache();
    }

    /**
     * Add an ORDER BY clause (or an additional ordering term) to this query
     *
     * @param orders one or more ordering terms
     * @return this Query object, to allow chaining method calls
     */
    public Query orderBy(Order... orders) {
        if (immutable) {
            return fork().orderBy(orders);
        }
        if (this.orders == null) {
            this.orders = new ArrayList<>();
        }
        SquidUtilities.addAll(this.orders, orders);
        invalidateCompileCache();
        return this;
    }

    /**
     * Set the limit of this statement. Using a negative value removes the limit.
     *
     * @param limit the maximum number of rows this query should return
     * @return this Query object, to allow chaining method calls
     */
    public Query limit(int limit) {
        return limit(limit < 0 ? NO_LIMIT : Field.<Integer>field(Integer.toString(limit)));
    }

    /**
     * Set the limit and offset of this statement. Use a negative value for limit to remove the limit. Use a value less
     * than one for offset to remove the offset.
     *
     * @param limit the maximum number of rows this query should return
     * @param offset the number of rows this query should skip
     * @return this Query object, to allow chaining method calls
     */
    public Query limit(int limit, int offset) {
        return limit(limit < 0 ? NO_LIMIT : Field.<Integer>field(Integer.toString(limit)),
                offset < 1 ? NO_OFFSET : Field.<Integer>field(Integer.toString(offset)));
    }

    /**
     * Set the limit of this statement as a SQL expression; e.g. a {@link Function} or the result of
     * {@link #asFunction()} to use a subquery. Use {@link #NO_LIMIT} to remove the limit.
     *
     * @param limit the maximum number of rows this query should return
     * @return this Query object, to allow chaining method calls
     */
    public Query limit(Field<Integer> limit) {
        if (limit == null) {
            limit = NO_LIMIT;
        }
        if (immutable) {
            return fork().limit(limit);
        }
        if (!this.limit.equals(limit)) {
            this.limit = limit;
            invalidateCompileCache();
        }
        return this;
    }

    /**
     * Set the limit of this statement as a SQL expression; e.g. a {@link Function} or the result of
     * {@link #asFunction()} to use a subquery. Use {@link #NO_LIMIT} for limit to remove the limit. Use
     * {@link #NO_OFFSET} for offset to remove the offset.
     *
     * @param limit the maximum number of rows this query should return
     * @param offset the number of rows this query should skip
     * @return this Query object, to allow chaining method calls
     */
    public Query limit(Field<Integer> limit, Field<Integer> offset) {
        if (limit == null) {
            limit = NO_LIMIT;
        }
        if (offset == null) {
            offset = NO_OFFSET;
        }
        if (immutable) {
            return fork().limit(limit, offset);
        }
        if (!this.limit.equals(limit) || !this.offset.equals(offset)) {
            this.limit = limit;
            this.offset = offset;
            invalidateCompileCache();
        }
        return this;
    }

    /**
     * @return the current limit of this query
     */
    public Field<Integer> getLimit() {
        return limit;
    }

    /**
     * @return the current offset of this query
     */
    public Field<Integer> getOffset() {
        return offset;
    }

    /**
     * Mark that this query should be checked for syntactic anomalies in the WHERE clause (e.g. if a raw selection was
     * applied)
     */
    public void requestValidation() {
        this.needsValidation = true;
    }

    /**
     * @return true if this query should be checked for syntactic anomalies in the WHERE clause
     */
    public boolean needsValidation() {
        return needsValidation;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && this.toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation) {
        visitSelectClause(builder, forSqlValidation);
        visitFromClause(builder, forSqlValidation);
        visitJoinClause(builder, forSqlValidation);
        visitWhereClause(builder, forSqlValidation);
        visitGroupByClause(builder, forSqlValidation);
        visitCompoundSelectClauses(builder, forSqlValidation);
        visitOrderByClause(builder, forSqlValidation);
        visitLimitClause(builder, forSqlValidation);

        if (needsValidation) {
            builder.setNeedsValidation();
        }
    }

    private void visitSelectClause(SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append("SELECT ");
        if (distinct) {
            builder.sql.append("DISTINCT ");
        }

        List<Field<?>> toSelect;
        if (isEmpty(fields)) {
            // SELECT * may yield unexpected column names, so we get the full list of fields to specify explicit aliases
            toSelect = getFields();
        } else {
            toSelect = fields;
        }

        builder.appendConcatenatedCompilables(toSelect, ", ", forSqlValidation);
    }

    private void visitFromClause(SqlBuilder builder, boolean forSqlValidation) {
        if (table == null) {
            return;
        }
        builder.sql.append(" FROM ");
        table.appendToSqlBuilder(builder, forSqlValidation);
    }

    private void visitJoinClause(SqlBuilder builder, boolean forSqlValidation) {
        if (isEmpty(joins)) {
            return;
        }
        builder.sql.append(" ");
        builder.appendConcatenatedCompilables(joins, " ", forSqlValidation);
    }

    private void visitWhereClause(SqlBuilder builder, boolean forSqlValidation) {
        if (isEmpty(criterions)) {
            return;
        }
        builder.sql.append(" WHERE ");
        if (forSqlValidation) {
            builder.sql.append("(");
        }
        builder.appendConcatenatedCompilables(criterions, " AND ", forSqlValidation);
        if (forSqlValidation) {
            builder.sql.append(")");
        }
    }

    private void visitGroupByClause(SqlBuilder builder, boolean forSqlValidation) {
        if (isEmpty(groupByFields)) {
            return;
        }
        builder.sql.append(" GROUP BY");
        for (Field<?> groupBy : groupByFields) {
            builder.sql.append(" ");
            groupBy.appendQualifiedExpression(builder, forSqlValidation);
            builder.sql.append(",");
        }
        builder.sql.deleteCharAt(builder.sql.length() - 1);
        if (isEmpty(havings)) {
            return;
        }
        builder.sql.append(" HAVING ");
        builder.appendConcatenatedCompilables(havings, " AND ", forSqlValidation);
    }

    private void visitCompoundSelectClauses(SqlBuilder builder, boolean forSqlValidation) {
        if (isEmpty(compoundSelects)) {
            return;
        }
        builder.sql.append(" ");
        builder.appendConcatenatedCompilables(compoundSelects, " ", forSqlValidation);
    }

    private void visitOrderByClause(SqlBuilder builder, boolean forSqlValidation) {
        if (isEmpty(orders)) {
            return;
        }
        builder.sql.append(" ORDER BY ");
        builder.appendConcatenatedCompilables(orders, ", ", forSqlValidation);
    }

    private void visitLimitClause(SqlBuilder builder, boolean forSqlValidation) {
        if (!NO_LIMIT.equals(limit) || !NO_OFFSET.equals(offset)) {
            builder.sql.append(" LIMIT ");
            limit.appendQualifiedExpression(builder, forSqlValidation);
            if (!NO_OFFSET.equals(offset)) {
                builder.sql.append(" OFFSET ");
                offset.appendQualifiedExpression(builder, forSqlValidation);
            }
        }
    }

    /**
     * @return the table being selected from
     */
    public SqlTable<?> getTable() {
        return this.table;
    }

    /**
     * @return true if a table has been specified
     */
    public boolean hasTable() {
        return this.table != null;
    }

    /**
     * Alias this query, for use when creating a subquery to select from
     *
     * @param alias the name for the table when this query is used as a subquery
     * @return a {@link SubqueryTable} from this Query
     */
    public SubqueryTable as(String alias) {
        return SubqueryTable.fromQuery(this, alias);
    }

    /**
     * Alias this query, for use when creating a subquery to select from
     *
     * @param alias the name for the table when this query is used as a subquery
     * @param modelClass the model class representing the subquery
     * @return a {@link SubqueryTable} from this Query
     */
    public SubqueryTable as(String alias, Class<? extends ViewModel> modelClass, Property<?>[] properties) {
        return SubqueryTable.fromQuery(this, alias, modelClass, properties);
    }

    /**
     * Return this query wrapped in a Function object, making it suitable for inclusion in another SELECT clause as a
     * subquery or for constructing {@link Criterion}s. Note: the query must have exactly one column in its
     * result set (i.e. one field in the SELECT clause) for this to be valid SQL.
     *
     * @return a {@link Function} from this query
     */
    public <T> Function<T> asFunction() {
        return Function.fromQuery(this);
    }

    /**
     * @return a new query initialized with the current state of this query. The copy is not entirely shallow--methods
     * called on one query will not affect the state of the forked query--but changes to variable arguments in
     * {@link Criterion Criterions} they share will affect both copies.
     */
    public Query fork() {
        Query newQuery = new Query(fields);
        newQuery.table = table;
        newQuery.criterions = forkList(criterions);
        newQuery.joins = forkList(joins);
        newQuery.groupByFields = forkList(groupByFields);
        newQuery.compoundSelects = forkList(compoundSelects);
        newQuery.orders = forkList(orders);
        newQuery.havings = forkList(havings);
        newQuery.limit = limit;
        newQuery.offset = offset;
        newQuery.distinct = distinct;
        newQuery.needsValidation = needsValidation;
        return newQuery;
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    private <T> ArrayList<T> forkList(ArrayList<T> list) {
        if (isEmpty(list)) {
            return null;
        }
        return new ArrayList<>(list);
    }

    /**
     * Makes the query immutable-ish. Future method calls on this Query object that would mutate it will instead
     * {@link #fork() fork} this one, mutate the copy, and return that instead. However, any variable arguments (e.g.
     * AtomicReferences in {@link Criterion Criterions}) may still change.
     *
     * @return this Query object
     */
    public Query freeze() {
        this.immutable = true;
        return this;
    }

    /**
     * @return true if {@link #freeze()} has been called on this object; false otherwise.
     */
    public boolean isImmutable() {
        return immutable;
    }

    /**
     * @return the {@link Field Fields} this query selects
     */
    public List<Field<?>> getFields() {
        if (isEmpty(selectAllCache)) {
            if (selectAllCache == null) {
                selectAllCache = new ArrayList<>();
            }
            if (!isEmpty(fields)) {
                selectAllCache.addAll(fields);
            } else {
                SquidUtilities.addAll(selectAllCache, table.allFields());
                if (joins != null) {
                    for (Join join : joins) {
                        SquidUtilities.addAll(selectAllCache, join.joinTable.allFields());
                    }
                }
            }
        }
        return new ArrayList<>(selectAllCache);
    }

}
