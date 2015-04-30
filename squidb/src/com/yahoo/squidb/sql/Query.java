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
    public static final int NO_LIMIT = -1;
    /** Specifies this query has no offset */
    public static final int NO_OFFSET = 0;

    private SqlTable<?> table = null;
    private ArrayList<Field<?>> fields = null;
    private ArrayList<Criterion> criterions = null;
    private ArrayList<Join> joins = null;
    private ArrayList<Field<?>> groupBies = null;
    private ArrayList<Criterion> havings = null;
    private ArrayList<CompoundSelect> compoundSelects = null;
    private ArrayList<Order> orders = null;
    private int limit = NO_LIMIT;
    private int offset = NO_OFFSET;
    private boolean distinct = false;
    private boolean immutable = false;

    private boolean needsValidation = false;
    private boolean parenthesizeWhere = false;

    private ArrayList<Field<?>> selectAllCache = null;

    private Query(List<Field<?>> fields) {
        if (!isEmpty(fields)) {
            this.fields = new ArrayList<Field<?>>(fields);
        }
    }

    private Query(Field<?>... fields) {
        if (!isEmpty(fields)) {
            this.fields = new ArrayList<Field<?>>();
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
                this.fields = new ArrayList<Field<?>>();
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
                this.fields = new ArrayList<Field<?>>(fields);
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
            updateNeedsValidationFromTable(table);
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
            this.joins = new ArrayList<Join>();
        }
        SquidUtilities.addAll(this.joins, joins);
        for (Join join : joins) {
            updateNeedsValidationFromTable(join.joinTable);
        }
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
        if (immutable) {
            return fork().where(criterion);
        }
        if (criterions == null) {
            criterions = new ArrayList<Criterion>();
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
        if (this.groupBies == null) {
            this.groupBies = new ArrayList<Field<?>>();
        }
        SquidUtilities.addAll(this.groupBies, fields);
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
        if (immutable) {
            return fork().having(criterion);
        }
        if (this.havings == null) {
            this.havings = new ArrayList<Criterion>();
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
            this.compoundSelects = new ArrayList<CompoundSelect>();
        }
        this.compoundSelects.add(compoundSelect);
        invalidateCompileCache();
        if (compoundSelect.query.needsValidation()) {
            requestValidation();
        }
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
            this.orders = new ArrayList<Order>();
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
        if (immutable) {
            return fork().limit(limit);
        }
        if (this.limit != limit) {
            this.limit = limit;
            invalidateCompileCache();
        }
        return this;
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
        if (immutable) {
            return fork().limit(limit, offset);
        }
        if (this.limit != limit || this.offset != offset) {
            this.limit = limit;
            this.offset = offset;
            invalidateCompileCache();
        }
        return this;
    }

    /**
     * @return the current limit of this query
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @return the current offset of this query
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Set whether to use additional parentheses surrounding the WHERE clause of this query. This setting is propagated
     * to subqueries in the FROM and JOIN clauses as well as any compound select clauses.
     *
     * @param parenthesize true to surround the WHERE clause with parentheses, false otherwise
     */
    public void parenthesizeWhere(boolean parenthesize) {
        if (this.parenthesizeWhere != parenthesize) {
            this.parenthesizeWhere = parenthesize;
            invalidateCompileCache();
        }

        if (table instanceof SubqueryTable) {
            ((SubqueryTable) table).query.parenthesizeWhere(parenthesize);
        }
        if (joins != null) {
            for (Join join : joins) {
                if (join.joinTable instanceof SubqueryTable) {
                    ((SubqueryTable) join.joinTable).query.parenthesizeWhere(parenthesize);
                }
            }
        }
        if (compoundSelects != null) {
            for (CompoundSelect compoundSelect : compoundSelects) {
                compoundSelect.query.parenthesizeWhere(parenthesize);
            }
        }
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

    private void updateNeedsValidationFromTable(SqlTable<?> sqlTable) {
        if (sqlTable instanceof SubqueryTable) {
            if (((SubqueryTable) sqlTable).query.needsValidation()) {
                requestValidation();
            }
        }
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
    protected void appendCompiledStringWithArguments(StringBuilder sql, List<Object> selectionArgsBuilder) {
        visitSelectClause(sql, selectionArgsBuilder);
        visitFromClause(sql, selectionArgsBuilder);
        visitJoinClause(sql, selectionArgsBuilder);
        visitWhereClause(sql, selectionArgsBuilder);
        visitGroupByClause(sql, selectionArgsBuilder);
        visitCompoundSelectClauses(sql, selectionArgsBuilder);
        visitOrderByClause(sql, selectionArgsBuilder);
        visitLimitClause(sql);
    }

    private void visitSelectClause(StringBuilder sql, List<Object> selectionArgsBuilder) {
        sql.append("SELECT ");
        if (distinct) {
            sql.append("DISTINCT ");
        }

        if (isEmpty(fields)) {
            sql.append("*");
            return;
        }
        SqlUtils.appendConcatenatedCompilables(fields, sql, selectionArgsBuilder, ", ");
    }

    private void visitFromClause(StringBuilder sql, List<Object> selectionArgsBuilder) {
        if (table == null) {
            return;
        }
        sql.append(" FROM ");
        table.appendCompiledStringWithArguments(sql, selectionArgsBuilder);
    }

    private void visitJoinClause(StringBuilder sql, List<Object> selectionArgsBuilder) {
        if (isEmpty(joins)) {
            return;
        }
        sql.append(" ");
        SqlUtils.appendConcatenatedCompilables(joins, sql, selectionArgsBuilder, " ");
    }

    private void visitWhereClause(StringBuilder sql, List<Object> selectionArgsBuilder) {
        if (isEmpty(criterions)) {
            return;
        }
        sql.append(" WHERE ");
        if (parenthesizeWhere) {
            sql.append("(");
        }
        SqlUtils.appendConcatenatedCompilables(criterions, sql, selectionArgsBuilder, " AND ");
        if (parenthesizeWhere) {
            sql.append(")");
        }
    }

    private void visitGroupByClause(StringBuilder sql, List<Object> selectionArgsBuilder) {
        if (isEmpty(groupBies)) {
            return;
        }
        sql.append(" GROUP BY");
        for (Field<?> groupBy : groupBies) {
            sql.append(" ");
            groupBy.appendQualifiedExpression(sql, selectionArgsBuilder);
            sql.append(",");
        }
        sql.deleteCharAt(sql.length() - 1);
        if (isEmpty(havings)) {
            return;
        }
        sql.append(" HAVING ");
        SqlUtils.appendConcatenatedCompilables(havings, sql, selectionArgsBuilder, " AND ");
    }

    private void visitCompoundSelectClauses(StringBuilder sql, List<Object> selectionArgsBuilder) {
        if (isEmpty(compoundSelects)) {
            return;
        }
        sql.append(" ");
        SqlUtils.appendConcatenatedCompilables(compoundSelects, sql, selectionArgsBuilder, " ");
    }

    private void visitOrderByClause(StringBuilder sql, List<Object> selectionArgsBuilder) {
        if (isEmpty(orders)) {
            return;
        }
        sql.append(" ORDER BY ");
        SqlUtils.appendConcatenatedCompilables(orders, sql, selectionArgsBuilder, ", ");
    }

    private void visitLimitClause(StringBuilder sql) {
        if (limit > NO_LIMIT || offset > NO_OFFSET) {
            sql.append(" LIMIT ").append(limit);
            if (offset > NO_OFFSET) {
                sql.append(" OFFSET ").append(offset);
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
     * @return a new query initialized with the current state of this query. The copy is not entirely shallow--methods
     * called on one query will not affect the state of the forked query--but changes to variable arguments in
     * {@link Criterion Criterions} they share will affect both copies.
     */
    public Query fork() {
        Query newQuery = new Query(fields);
        newQuery.table = table;
        newQuery.criterions = forkList(criterions);
        newQuery.joins = forkList(joins);
        newQuery.groupBies = forkList(groupBies);
        newQuery.compoundSelects = forkList(compoundSelects);
        newQuery.orders = forkList(orders);
        newQuery.havings = forkList(havings);
        newQuery.limit = limit;
        newQuery.offset = offset;
        newQuery.distinct = distinct;
        newQuery.needsValidation = needsValidation;
        newQuery.parenthesizeWhere = parenthesizeWhere;
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
        return new ArrayList<T>(list);
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
     * @return the {@link Field Fields} this query selects
     */
    public List<Field<?>> getFields() {
        if (isEmpty(selectAllCache)) {
            if (selectAllCache == null) {
                selectAllCache = new ArrayList<Field<?>>();
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
        return new ArrayList<Field<?>>(selectAllCache);
    }

}
