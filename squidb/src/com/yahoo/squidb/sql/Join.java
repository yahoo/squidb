/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

/**
 * A JOIN clause used in a SELECT statement.
 * <p>
 * All joins in SQLite are based on the cartesian product of the left and right-hand datasets. When no join constraint
 * is specified, the result is formed by combining each unique combination of a row from the left-hand and right-hand
 * datasets. If a join constraint is specified, it is evaluated for each row of the cartesian product as a boolean
 * expression, and only rows for which the expression evaluates to true are included in the result.
 */
public class Join extends CompilableWithArguments {

    private enum JoinType {
        INNER, LEFT, CROSS
    }

    final SqlTable<?> joinTable;
    private final JoinType joinType;
    private final Criterion[] criterions;
    private final Property<?>[] usings;

    private Join(SqlTable<?> table, JoinType joinType, Criterion... criterions) {
        this.joinTable = table;
        this.joinType = joinType;
        this.criterions = criterions;
        this.usings = null;
    }

    private Join(SqlTable<?> table, JoinType joinType, Property<?>... usingColumns) {
        this.joinTable = table;
        this.joinType = joinType;
        this.usings = usingColumns;
        this.criterions = null;
    }

    /**
     * Construct an INNER join with an ON clause.
     *
     * @param table the table to join on
     * @param criterions criterions to use for the ON clause
     */
    public static Join inner(SqlTable<?> table, Criterion... criterions) {
        return new Join(table, JoinType.INNER, criterions);
    }

    /**
     * Construct an INNER join with a USING clause.
     *
     * @param table the table to join on
     * @param usingColumns columns to use for the USING clause
     */
    public static Join inner(SqlTable<?> table, Property<?>... usingColumns) {
        return new Join(table, JoinType.INNER, usingColumns);
    }

    /**
     * Construct a LEFT join with an ON clause. Left joins return all the rows an INNER join would return, plus an
     * extra row for each row in the left-hand dataset that corresponds to no rows at all in the composite dataset (if
     * any). The added rows contain NULL values in the columns from the right-hand dataset.
     *
     * @param table the table to join on
     * @param criterions criterions to use for the ON clause
     */
    public static Join left(SqlTable<?> table, Criterion... criterions) {
        return new Join(table, JoinType.LEFT, criterions);
    }

    /**
     * Construct a LEFT join with a USING clause. Left joins return all the rows an INNER join would return, plus an
     * extra row for each row in the left-hand dataset that corresponds to no rows at all in the composite dataset (if
     * any). The added rows contain NULL values in the columns from the right-hand dataset.
     *
     * @param table the table to join on
     * @param usingColumns columns to use for the USING clause
     */
    public static Join left(SqlTable<?> table, Property<?>... usingColumns) {
        return new Join(table, JoinType.LEFT, usingColumns);
    }

    /**
     * Construct a CROSS join with an ON clause. A CROSS join produces the same result as an INNER join, but prohibits
     * the query optimizer from reordering the tables in the join. Avoid using CROSS join except in specific situations
     * where manual control of the query optimizer is desired.
     *
     * @param table the table to join on
     * @param criterions criterions to use for the ON clause
     */
    public static Join cross(SqlTable<?> table, Criterion... criterions) {
        return new Join(table, JoinType.CROSS, criterions);
    }

    /**
     * Construct a CROSS join with a USING clause. A CROSS join produces the same result as an INNER join, but
     * prohibits the query optimizer from reordering the tables in the join. Avoid using CROSS join except in specific
     * situations where manual control of the query optimizer is desired.
     *
     * @param table the table to join on
     * @param usingColumns columns to use for the USING clause
     */
    public static Join cross(SqlTable<?> table, Property<?>... usingColumns) {
        return new Join(table, JoinType.CROSS, usingColumns);
    }

    @Override
    void appendToSqlBuilder(SqlBuilder builder, boolean forSqlValidation) {
        builder.sql.append(joinType).append(" JOIN ");
        joinTable.appendToSqlBuilder(builder, forSqlValidation);
        builder.sql.append(" ");
        if (criterions != null && criterions.length > 0) {
            builder.sql.append("ON ");
            for (int i = 0; i < criterions.length; i++) {
                if (i > 0) {
                    builder.sql.append(" AND ");
                }
                criterions[i].appendToSqlBuilder(builder, forSqlValidation);
            }
        } else if (usings != null && usings.length > 0) {
            builder.sql.append("USING (");
            for (int i = 0; i < usings.length; i++) {
                if (i > 0) {
                    builder.sql.append(", ");
                }
                builder.sql.append(usings[i].getExpression());
            }
            builder.sql.append(")");
        }
    }
}
