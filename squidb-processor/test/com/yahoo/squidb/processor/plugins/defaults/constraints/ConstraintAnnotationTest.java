/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.constraints;

import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.PropertyGenerator;
import com.yahoo.squidb.processor.test.SquidbProcessorTestCase;

import java.lang.annotation.Annotation;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConstraintAnnotationTest extends SquidbProcessorTestCase {

    protected <A extends Annotation> PropertyGenerator mockPropertyGeneratorWithAnnotation(Class<A> annotationClass,
            A annotation) {
        PropertyGenerator propertyGenerator = mock(PropertyGenerator.class);
        VariableElement field = mock(VariableElement.class);
        when(propertyGenerator.getField()).thenReturn(field);
        when(field.getAnnotation(annotationClass)).thenReturn(annotation);
        return propertyGenerator;
    }

    protected <A extends Annotation>TableModelSpecWrapper mockTableModelSpecWithAnnotation(Class<A> annotationClass,
            A annotation) {
        TableModelSpecWrapper modelSpec = mock(TableModelSpecWrapper.class);
        TypeElement modelSpecElement = mock(TypeElement.class);
        when(modelSpec.getModelSpecElement()).thenReturn(modelSpecElement);
        when(modelSpecElement.getAnnotation(annotationClass)).thenReturn(annotation);
        return modelSpec;
    }

}
