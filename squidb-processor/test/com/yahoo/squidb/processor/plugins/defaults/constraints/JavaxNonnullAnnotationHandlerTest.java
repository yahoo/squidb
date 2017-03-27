/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.processing.Messager;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JavaxNonnullAnnotationHandlerTest extends SimpleColumnConstraintAnnotationTest<Nonnull> {

    @Test
    public void testUsingJavaxNonnullLogsWarning() {
        ColumnConstraintAnnotationHandler<Nonnull> handler = getColumnAnnotationHandler();
        PropertyGenerator mockPropertyGenerator = mockPropertyGeneratorWithAnnotation(Nonnull.class,
                mock(Nonnull.class));
        handler.validateAnnotationForColumn(mockPropertyGenerator, mock(ModelSpec.class), pluginEnv);
        Messager messager = pluginEnv.getMessager();
        VariableElement field = mockPropertyGenerator.getField();
        verify(messager).printMessage(eq(Diagnostic.Kind.WARNING), anyString(), eq(field));
    }

    @Override
    protected ColumnConstraintAnnotationHandler<Nonnull> getColumnAnnotationHandler() {
        return new JavaxNonnullAnnotationHandler();
    }

    @Override
    protected Class<Nonnull> getAnnotationClass() {
        return Nonnull.class;
    }

    @Override
    protected List<Nonnull> getMockedAnnotationMatrix() {
        return Collections.singletonList(mock(Nonnull.class));
    }

    @Override
    protected List<String> getExpectedConstraintStrings() {
        return Collections.singletonList(" NOT NULL");
    }
}
