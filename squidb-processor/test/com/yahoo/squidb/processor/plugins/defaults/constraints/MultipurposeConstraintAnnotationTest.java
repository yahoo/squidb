/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import org.junit.Test;

import java.lang.annotation.Annotation;

import javax.lang.model.element.Element;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

abstract class MultipurposeConstraintAnnotationTest<A extends Annotation> extends ConstraintAnnotationTest {

    @Test
    public void testColumnConstraintIsAppendedToColumnDefinition() {
        ColumnConstraintAnnotationHandler<A> handler = getColumnAnnotationHandler();
        testColumnAppendWithAnnotationValue(handler, "test");
        testColumnAppendWithAnnotationValue(handler, " test ");
        testColumnAppendWithAnnotationValue(handler, " test test ");
        testColumnAppendWithAnnotationValue(handler, " ( test test ) ");
    }

    private void testColumnAppendWithAnnotationValue(ColumnConstraintAnnotationHandler<A> handler,
            String annotationValue) {
        StringBuilder builder = new StringBuilder();
        handler.appendConstraintForColumn(builder, getMockedPropertyGenerator(annotationValue), pluginEnv);
        assertEquals(getExpectedConstraintString(annotationValue), builder.toString().trim());
    }

    @Test
    public void testColumnConstraintEmptyLogsValidationError() {
        ColumnConstraintAnnotationHandler<A> handler = getColumnAnnotationHandler();
        testColumnValidationWithAnnotationValue(handler, "", true);
        testColumnValidationWithAnnotationValue(handler, "    ", true);
        testColumnValidationWithAnnotationValue(handler, "\n", true);
        testColumnValidationWithAnnotationValue(handler, "test", false);
        testColumnValidationWithAnnotationValue(handler, " test ", false);
        testColumnValidationWithAnnotationValue(handler, " test test ", false);
        testColumnValidationWithAnnotationValue(handler, " ( test test ) ", false);
    }

    private void testColumnValidationWithAnnotationValue(ColumnConstraintAnnotationHandler<A> handler,
            String annotationValue, boolean expectError) {
        TableModelSpecWrapper modelSpec = mock(TableModelSpecWrapper.class);
        handler.validateAnnotationForColumn(getMockedPropertyGenerator(annotationValue), modelSpec, pluginEnv);
        verify(modelSpec, times(expectError ? 1 : 0)).logError(anyString(), any(Element.class));
    }

    private PropertyGenerator getMockedPropertyGenerator(String annotationValue) {
        return mockPropertyGeneratorWithAnnotation(getAnnotationClass(), getMockedAnnotation(annotationValue));
    }

    @Test
    public void testTableConstraintIsAppendedToTableDefinition() {
        TableConstraintAnnotationHandler<A> handler = getTableAnnotationHandler();
        testTableAppendWithAnnotationValue(handler, "test");
        testTableAppendWithAnnotationValue(handler, " test ");
        testTableAppendWithAnnotationValue(handler, " test test ");
        testTableAppendWithAnnotationValue(handler, " ( test test ) ");
    }

    private void testTableAppendWithAnnotationValue(TableConstraintAnnotationHandler<A> handler,
            String annotationValue) {
        StringBuilder builder = new StringBuilder();
        TableModelSpecWrapper modelSpec = getMockedTableModelSpec(annotationValue);
        handler.appendConstraintForTable(builder, modelSpec, pluginEnv);
        assertEquals(getExpectedConstraintString(annotationValue), builder.toString().trim());
    }

    @Test
    public void testTableConstraintEmptyLogsValidationError() {
        TableConstraintAnnotationHandler<A> handler = getTableAnnotationHandler();
        testTableValidationWithAnnotationValue(handler, "", true);
        testTableValidationWithAnnotationValue(handler, "    ", true);
        testTableValidationWithAnnotationValue(handler, "\n", true);
        testTableValidationWithAnnotationValue(handler, "test", false);
        testTableValidationWithAnnotationValue(handler, " test ", false);
        testTableValidationWithAnnotationValue(handler, " test test ", false);
        testTableValidationWithAnnotationValue(handler, " ( test test ) ", false);
    }

    private void testTableValidationWithAnnotationValue(TableConstraintAnnotationHandler<A> handler,
            String annotationValue, boolean expectError) {
        TableModelSpecWrapper modelSpec = getMockedTableModelSpec(annotationValue);
        handler.validateAnnotationForTable(modelSpec, pluginEnv);
        verify(modelSpec, times(expectError ? 1 : 0)).logError(anyString(), any(Element.class));
    }

    private TableModelSpecWrapper getMockedTableModelSpec(String annotationValue) {
        return mockTableModelSpecWithAnnotation(getAnnotationClass(), getMockedAnnotation(annotationValue));
    }

    protected abstract ColumnConstraintAnnotationHandler<A> getColumnAnnotationHandler();

    protected abstract TableConstraintAnnotationHandler<A> getTableAnnotationHandler();

    protected abstract Class<A> getAnnotationClass();

    protected abstract A getMockedAnnotation(String value);

    protected abstract String getExpectedConstraintString(String value);
}
