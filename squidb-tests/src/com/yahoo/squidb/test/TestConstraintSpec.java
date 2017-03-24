/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.tables.ColumnName;
import com.yahoo.squidb.annotations.tables.IndexOrder;
import com.yahoo.squidb.annotations.tables.constraints.Check;
import com.yahoo.squidb.annotations.tables.constraints.Collate;
import com.yahoo.squidb.annotations.tables.constraints.ConstraintSql;
import com.yahoo.squidb.annotations.tables.constraints.IndexedColumn;
import com.yahoo.squidb.annotations.tables.constraints.NotNull;
import com.yahoo.squidb.annotations.tables.constraints.PrimaryKeyColumns;
import com.yahoo.squidb.annotations.tables.constraints.TableConstraints;
import com.yahoo.squidb.annotations.tables.constraints.Unique;
import com.yahoo.squidb.annotations.tables.constraints.UniqueColumns;
import com.yahoo.squidb.annotations.tables.defaults.DefaultBlob;
import com.yahoo.squidb.annotations.tables.defaults.DefaultBoolean;
import com.yahoo.squidb.annotations.tables.defaults.DefaultDouble;
import com.yahoo.squidb.annotations.tables.defaults.DefaultInt;
import com.yahoo.squidb.annotations.tables.defaults.DefaultLong;

import static com.yahoo.squidb.annotations.tables.ConflictAlgorithm.ABORT;
import static com.yahoo.squidb.annotations.tables.ConflictAlgorithm.FAIL;
import static com.yahoo.squidb.annotations.tables.ConflictAlgorithm.IGNORE;
import static com.yahoo.squidb.annotations.tables.ConflictAlgorithm.ROLLBACK;
import static com.yahoo.squidb.annotations.tables.constraints.Collate.NOCASE;

/**
 * This schema exists only as a sanity check that all of our constraint annotations play nicely when combined
 */
@TableModelSpec(className = "TestConstraint", tableName = "testConstraints")
@TableConstraints(checks = {@Check("some_long > 0"), @Check("(some_int < 0)")},
        constraintSqls = {@ConstraintSql("UNIQUE(some_long, some_boolean)"), @ConstraintSql("CHECK(some_double != 1)")},
        uniques = {@UniqueColumns(columns = {"some_long", "some_string"}),
                @UniqueColumns(indexedColumns = {@IndexedColumn(name = "some_string", collate = Collate.NOCASE,
                        order = IndexOrder.DESC)})})
@UniqueColumns(columns = {"some_double"}, onConflict = ROLLBACK)
@Check("(some_boolean != 2)")
@ConstraintSql("CHECK(some_blob != x'')")
@PrimaryKeyColumns(indexedColumns = {@IndexedColumn(name = "some_string")})
public class TestConstraintSpec {

    @ColumnName("some_long")
    @NotNull @Unique(onConflict = ABORT) @DefaultLong(1)
    long someLong;

    @ColumnName("some_string")
    @NotNull @Collate(NOCASE)
    String someString;

    @ColumnName("some_int")
    @Unique(onConflict = FAIL) @DefaultInt(-1)
    int someInt;

    @ColumnName("some_double")
    @Check("some_double != 0.0") @DefaultDouble(1.5)
    double someDouble;

    @ColumnName("some_boolean")
    @DefaultBoolean(true)
    boolean someBoolean;

    @ColumnName("some_blob")
    @NotNull @Unique(onConflict = IGNORE) @DefaultBlob("X'ABCDEF'")
    byte[] someBlob;
}
