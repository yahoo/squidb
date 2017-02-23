/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.List;

import static org.junit.Assert.assertEquals;

abstract class SimpleColumnConstraintAnnotationTest<A extends Annotation> extends ConstraintAnnotationTest {

    @Test
    public void testConstraintIsAppendedToColumnDefinition() {
        ColumnConstraintAnnotationHandler<A> handler = getColumnAnnotationHandler();
        List<A> annotationMatrix = getMockedAnnotationMatrix();
        List<String> constraintStrings = getExpectedConstraintStrings();
        assertEquals(annotationMatrix.size(), constraintStrings.size());
        for (int i = 0; i < annotationMatrix.size(); i++) {
            testColumnAppendWithAnnotationValue(handler, annotationMatrix.get(i), constraintStrings.get(i));
        }
    }

    private void testColumnAppendWithAnnotationValue(ColumnConstraintAnnotationHandler<A> handler, A annotation,
            String expectedConstraintString) {
        StringBuilder builder = new StringBuilder();
        handler.appendConstraintForColumn(builder,
                mockPropertyGeneratorWithAnnotation(getAnnotationClass(), annotation), pluginEnv);
        assertEquals(expectedConstraintString, builder.toString());
    }

    protected abstract ColumnConstraintAnnotationHandler<A> getColumnAnnotationHandler();

    protected abstract Class<A> getAnnotationClass();

    protected abstract List<A> getMockedAnnotationMatrix();

    protected abstract List<String> getExpectedConstraintStrings();

}
