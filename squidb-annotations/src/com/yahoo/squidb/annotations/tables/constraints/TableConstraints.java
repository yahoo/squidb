/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables.constraints;

/**
 * Container annotation that holds lists of other table constraint annotations, since SQLite allows specifying
 * multiple constraints of the same type at the table level. Currently supports {@link Check}, {@link ConstraintSql},
 * and {@link UniqueColumns} annotations. Does not support {@link PrimaryKeyColumns} because only one primary key
 * constraint is allowed per table; the {@link PrimaryKeyColumns} annotation can target the model spec class directly.
 */
public @interface TableConstraints {

    /**
     * Container for multiple {@link Check} annotations
     */
    Check[] checks() default {};

    /**
     * Container for multiple {@link ConstraintSql} annotations
     */
    ConstraintSql[] constraintSqls() default {};

    /**
     * Container for multiple {@link UniqueColumns} annotations
     */
    UniqueColumns[] uniques() default {};

}
