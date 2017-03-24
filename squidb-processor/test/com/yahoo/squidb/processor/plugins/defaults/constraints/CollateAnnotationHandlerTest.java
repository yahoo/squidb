/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.annotations.tables.constraints.Collate;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.Element;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CollateAnnotationHandlerTest extends SimpleColumnConstraintAnnotationTest<Collate> {

    @Test
    public void testEmptyCollateValueLogsValidationError() {
        CollateAnnotationHandler handler = new CollateAnnotationHandler();
        testColumnValidationWithAnnotationValue(handler, "", true);
        testColumnValidationWithAnnotationValue(handler, "    ", true);
        testColumnValidationWithAnnotationValue(handler, "\n", true);
        testColumnValidationWithAnnotationValue(handler, "test", false);
        testColumnValidationWithAnnotationValue(handler, " test ", false);
        testColumnValidationWithAnnotationValue(handler, " test test ", false);
    }

    private void testColumnValidationWithAnnotationValue(CollateAnnotationHandler handler,
            String annotationValue, boolean expectError) {
        TableModelSpecWrapper modelSpec = mock(TableModelSpecWrapper.class);
        handler.validateAnnotationForColumn(mockPropertyGeneratorWithAnnotation(getAnnotationClass(),
                getMockedAnnotation(annotationValue)), modelSpec, pluginEnv);
        verify(modelSpec, times(expectError ? 1 : 0)).logError(anyString(), any(Element.class));
    }

    @Override
    protected ColumnConstraintAnnotationHandler<Collate> getColumnAnnotationHandler() {
        return new CollateAnnotationHandler();
    }

    @Override
    protected Class<Collate> getAnnotationClass() {
        return Collate.class;
    }

    @Override
    protected List<Collate> getMockedAnnotationMatrix() {
        List<Collate> result = new ArrayList<>();
        for (String value : getTestCollateValues()) {
            result.add(getMockedAnnotation(value));
        }
        return result;
    }

    private Collate getMockedAnnotation(String value) {
        Collate collate = mock(Collate.class);
        when(collate.value()).thenReturn(value);
        return collate;
    }

    @Override
    protected List<String> getExpectedConstraintStrings() {
        List<String> result = new ArrayList<>();
        for (String value : getTestCollateValues()) {
            result.add(" COLLATE " + value.trim());
        }
        return result;
    }

    private List<String> getTestCollateValues() {
        return Arrays.asList(Collate.BINARY, Collate.NOCASE, Collate.RTRIM, " BINARY ", " NOCASE ", " RTRIM ");
    }
}
