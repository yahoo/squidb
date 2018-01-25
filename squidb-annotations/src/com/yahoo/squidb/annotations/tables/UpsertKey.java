/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that the given column is part of a "logical key" for an upsertable model. A logical key is a column or
 * collection of columns that uniquely identifies a row in a table, but is generally distinct from the rowid. For
 * example, the logical key might be a single String column representing a guid provided by a server, or it might be
 * a collection of many columns that when taken together form a unique identifier for a row.
 * <p>
 * Model objects that declare one or more &#064;UpsertKey columns can be used with SquidDatabase.upsert(). The upsert()
 * method will look in the table for a row matching the logical key values in the given item. If one is found, the
 * values will be updated and persisted back to the database, otherwise a new row will be inserted.
 * <p>
 * In SquiDB's implementation of upsert, logical keys must always uniquely identify a row. To this end, there are two
 * requirements for logical key columns:
 * <ol>
 * <li>There must be a uniqueness constraint across the collection of columns making up the logical key. For single-column
 * logical keys, this can be specified using a &#064;Unique annotation on the column. For multi-column logical keys, this
 * can be specified at the table level using the &#064;UniqueColumns annotation.</li>
 * <li>Each individual logical key column must have a NOT NULL constraint, since NULL values are not unique in a SQLite
 * table even if there is a uniqueness constraint on the column. In order to avoid breaking uniqueness, all logical key
 * columns must therefore be NOT NULL. SquiDB will attempt to validate this as best it can by inspecting the
 * &#064;ColumnSpec annotation on the field for a NOT NULL constraint and logging an error if it doesn't find one.
 * However, this validation may not catch edge cases where users implement custom PropertyGenerators for fields that
 * override and/or ignore ColumnSpec's constraint handling. Most users will never have this problem, but those who
 * implement custom field handling plugins should be aware of this edge case.</li>
 * </ol>
 */
@Target(ElementType.FIELD)
public @interface UpsertKey {
}
