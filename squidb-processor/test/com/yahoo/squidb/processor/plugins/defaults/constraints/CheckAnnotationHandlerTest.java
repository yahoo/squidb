/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.constraints.Check;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckAnnotationHandlerTest extends MultipurposeConstraintAnnotationTest<Check> {

    @Override
    protected ColumnConstraintAnnotationHandler<Check> getColumnAnnotationHandler() {
        return new CheckAnnotationHandler.ColumnCheckAnnotationHandler();
    }

    @Override
    protected TableConstraintAnnotationHandler<Check> getTableAnnotationHandler() {
        return new CheckAnnotationHandler.TableCheckAnnotationHandler();
    }

    @Override
    protected Class<Check> getAnnotationClass() {
        return Check.class;
    }

    @Override
    protected Check getMockedAnnotation(String value) {
        Check check = mock(Check.class);
        when(check.value()).thenReturn(value);
        return check;
    }

    @Override
    protected String getExpectedConstraintString(String value) {
        String expression = value.trim();
        if (!expression.startsWith("(") || !expression.endsWith(")")) {
            expression = "(" + expression + ")";
        }
        return " CHECK" + expression;
    }
}
