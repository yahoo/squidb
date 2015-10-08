/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import com.yahoo.squidb.sql.Criterion;
import com.yahoo.squidb.sql.Field;
import com.yahoo.squidb.sql.Order;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.SqlUtils;
import com.yahoo.squidb.sql.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for building queries that use raw strings for projection, selection, and sort order. This is
 * particularly useful when these elements are provided by an outside party, e.g. through a
 * {@link android.content.ContentProvider#query(android.net.Uri, String[], String, String[], String)
 * ContentProvider query}. Use the set... methods to build its internal state, then call
 * {@link #build(String[], String, String[], String) build} with the raw arguments to construct an appropriate Query.
 * <p>
 * For maximum protection against SQL injection attacks, clients should do the following:
 * <ul>
 * <li>set strict mode to true</li>
 * <li>provide a {@link ProjectionMap}</li>
 * <li>call build() to create a Query that will be validated when executed by a
 * {@link com.yahoo.squidb.data.SquidDatabase}</li>
 * </ul>
 * These steps will ensure the query is protected against malicious arguments in the projection, selection, and
 * sortOrder.
 */
public class ContentProviderQueryBuilder {

    private boolean strictMode = false;
    private ProjectionMap projectionMap = null;
    private SqlTable<?> dataSource = null;
    private Order[] defaultOrder = null;

    /**
     * Construct a ContentProviderQueryBuilder
     */
    public ContentProviderQueryBuilder() {
    }

    /**
     * Construct a ContentProviderQueryBuilder initialized with a {@link ProjectionMap} from the given properties array
     * and data source. Typically, these would come from the same model class, e.g.
     * <pre>
     *     new ContentProviderQueryBuilder(ContractContact.PROPERTIES, ContractContact.SUBQUERY);
     * </pre>
     *
     * @param properties the properties to initialize the projection map with
     * @param dataSource the data source to select from
     */
    public ContentProviderQueryBuilder(Property<?>[] properties, SqlTable<?> dataSource) {
        ProjectionMap projectionMap = new ProjectionMap();
        projectionMap.putAll(properties);
        setProjectionMap(projectionMap);

        setDataSource(dataSource);
    }

    /**
     * When this flag is set and you build a {@link Query} using {@link #build(String[], String, String[], String)
     * build}, the following safeguards will be employed to protect against malicious SQL:
     * <ul>
     * <li>Raw selections are verified against syntactic anomalies</li>
     * <li>Elements in the raw projection that do not exist in the projection map will cause an exception to be thrown.
     * Note that if you are using strict mode, you must set a {@link ProjectionMap}.</li>
     * </ul>
     *
     * By default, this value is false.
     *
     * @param strict true to enable strict mode, false otherwise
     * @return this builder object, to allow chaining method calls
     */
    public ContentProviderQueryBuilder setStrict(boolean strict) {
        this.strictMode = strict;
        return this;
    }

    /**
     * Set the ProjectionMap that will be used when building queries with this builder
     *
     * @param projectionMap the ProjectionMap to use
     * @return this builder object, to allow chaining method calls
     */
    public ContentProviderQueryBuilder setProjectionMap(ProjectionMap projectionMap) {
        this.projectionMap = projectionMap;
        return this;
    }

    /**
     * Set the data source (e.g. a {@link Table}) to select from when building queries with this builder
     *
     * @param dataSource the data source to select from
     * @return this builder object, to allow chaining method calls
     */
    public ContentProviderQueryBuilder setDataSource(SqlTable<?> dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    /**
     * Set the default ordering terms to use when building queries with this builder
     *
     * @param orders the default ordering terms
     * @return this builder object, to allow chaining method calls
     */
    public ContentProviderQueryBuilder setDefaultOrder(Order... orders) {
        if (orders == null || orders.length == 0) {
            defaultOrder = null;
        } else {
            defaultOrder = orders;
        }
        return this;
    }

    /**
     * Build a {@link Query} combining this object's internal state with the arguments passed. If a
     * {@link ProjectionMap} is set, the projection elements will be evaluated and transformed accordingly. If the
     * sortOrder is null or empty, the default order will be used (if one was set).
     *
     * @param projection the raw column names to be selected
     * @param selection a raw selection string
     * @param selectionArgs array of strings which substitute replaceable arguments in the selection string
     * @param sortOrder a raw ordering clause
     * @return a {@link Query} using the projection, selection, selection args, and sort order
     */
    public Query build(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Query query = Query.select(computeProjection(projection)).from(dataSource);
        boolean hasUserSelection = !SqlUtils.isEmpty(selection);
        if (hasUserSelection) {
            query.where(Criterion.fromRawSelection(selection, selectionArgs));
        }
        if (!SqlUtils.isEmpty(sortOrder)) {
            query.orderBy(Order.fromExpression(sortOrder));
        } else if (defaultOrder != null && defaultOrder.length > 0) {
            query.orderBy(defaultOrder);
        }
        if (strictMode && hasUserSelection) {
            query.requestValidation();
        }
        return query;
    }

    /**
     * Compute a projection based on the raw projection and the current projection map (if one was set). Elements not
     * found in the projection map will throw an exception if strict mode was enabled, otherwise they are ignored. Note
     * that if you are using strict mode, you must provide a ProjectionMap that contains all the valid expressions that
     * can be included in the raw projection.
     *
     * @param projectionIn the raw projection
     * @return the transformed Fields to select
     */
    private List<Field<?>> computeProjection(String[] projectionIn) {
        // default selects all
        List<Field<?>> projection = null;
        if (projectionMap == null) {
            if (strictMode) {
                throw new IllegalStateException("Strict mode requires a projection map to be set");
            }
            if (projectionIn != null && projectionIn.length > 0) {
                // convert what we're given to Fields
                projection = new ArrayList<Field<?>>(projectionIn.length);
                for (String expression : projectionIn) {
                    projection.add(Field.field(expression));
                }
            }
        } else {
            if (projectionIn == null || projectionIn.length == 0) {
                projection = projectionMap.getDefaultProjection();
            } else {
                // convert projection elements, ignoring ones not in the projection map
                projection = new ArrayList<Field<?>>(projectionIn.length);
                for (String expression : projectionIn) {
                    Field<?> column = projectionMap.get(expression);
                    if (column != null) {
                        projection.add(column);
                    } else if (strictMode) {
                        throw new IllegalArgumentException("Invalid column: " + expression);
                    }
                }
            }
        }

        return projection;
    }
}
