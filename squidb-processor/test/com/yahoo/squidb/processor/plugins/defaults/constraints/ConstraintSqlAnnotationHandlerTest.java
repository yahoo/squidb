/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.constraints.ConstraintSql;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConstraintSqlAnnotationHandlerTest extends MultipurposeConstraintAnnotationTest<ConstraintSql> {

    @Override
    protected String getExpectedConstraintString(String value) {
        return value.trim();
    }

    @Override
    protected ColumnConstraintAnnotationHandler<ConstraintSql> getColumnAnnotationHandler() {
        return new ConstraintSqlAnnotationHandler.ColumnConstraintSqlAnnotationHandler();
    }

    @Override
    protected TableConstraintAnnotationHandler<ConstraintSql> getTableAnnotationHandler() {
        return new ConstraintSqlAnnotationHandler.TableConstraintSqlAnnotationHandler();
    }

    @Override
    protected Class<ConstraintSql> getAnnotationClass() {
        return ConstraintSql.class;
    }

    @Override
    protected ConstraintSql getMockedAnnotation(String value) {
        ConstraintSql constraintSql = mock(ConstraintSql.class);
        when(constraintSql.value()).thenReturn(value);
        return constraintSql;
    }


}
