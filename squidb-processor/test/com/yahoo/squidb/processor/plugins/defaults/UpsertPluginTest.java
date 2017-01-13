/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults;

import com.yahoo.squidb.annotations.UpsertKey;
import com.yahoo.squidb.processor.data.InheritedModelSpecWrapper;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.data.ViewModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;
import com.yahoo.squidb.processor.test.SquidbProcessorTestCase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpsertPluginTest extends SquidbProcessorTestCase {

    private static final String NOT_NULL = "NOT NULL";

    @Test
    public void testUpsertPluginOnlyAcceptsTableModelSpecs() {
        assertTrue(new UpsertPlugin().init(mock(TableModelSpecWrapper.class), pluginEnv));
        assertFalse(new UpsertPlugin().init(mock(ViewModelSpecWrapper.class), pluginEnv));
        assertFalse(new UpsertPlugin().init(mock(InheritedModelSpecWrapper.class), pluginEnv));
    }

    @Test
    public void testNotNullConstraintsEnforcedOnAllUpsertKeyColumns() {
        testNotNullConstraints(true, "col1", "NOT NULL");
        testNotNullConstraints(true, "col1", "NOT NULL", "col2", "not null");
        testNotNullConstraints(true, "col1", "    not     null    ");
        testNotNullConstraints(true, "col1", "NoT NuLl");

        testNotNullConstraints(false, "col1", null);
        testNotNullConstraints(false, "col1", "unique");
        testNotNullConstraints(false, "col1", "NOT NULL", "col2", null);
        testNotNullConstraints(false, "col1", "not nul", "col2", "not null");
    }

    private void testNotNullConstraints(boolean expectSuccess, String... namesAndConstraints) {
        StringBuilder uniquenessConstraint = new StringBuilder("UNIQUE(");
        for (int i = 0; i < namesAndConstraints.length; i += 2) {
            uniquenessConstraint.append(namesAndConstraints[i]).append(",");
        }
        uniquenessConstraint.deleteCharAt(uniquenessConstraint.length() - 1).append(")");
        testUpsertVerificationWithMockedModelSpec(
                getMockedModelSpec(uniquenessConstraint.toString(), namesAndConstraints), expectSuccess);
    }

    @Test
    public void testUniquenessConstraintEnforcedForSingleUpsertKeyColumns() {
        testSingleColumnUniqueness(false, null, null);
        testSingleColumnUniqueness(false, null, "NOT NULL");
        testSingleColumnUniqueness(true, null, "UNIQUE NOT NULL");
        testSingleColumnUniqueness(true, null, "PRIMARY KEY NOT NULL");
        testSingleColumnUniqueness(true, null, "unIqUE  NOT NULL");
        testSingleColumnUniqueness(true, null, "priMAry    KEY  NOT NULL");
        testSingleColumnUniqueness(false, null, "uniqu NOT NULL");
        testSingleColumnUniqueness(false, null, "primarykey NOT NULL");

        testSingleColumnUniqueness(false, null, NOT_NULL);
        testSingleColumnUniqueness(false, "", NOT_NULL);
        testSingleColumnUniqueness(true, "UNIQUE(col1)", NOT_NULL);
        testSingleColumnUniqueness(true, "PRIMARY KEY(col1)", NOT_NULL);
        testSingleColumnUniqueness(true, "uniQUe  (  col1  )", NOT_NULL);
        testSingleColumnUniqueness(true, "primary KEY (  col1  )", NOT_NULL);
        testSingleColumnUniqueness(true, "UNIQUE( col1 collate NOcase)", NOT_NULL);
        testSingleColumnUniqueness(true, "UNIQUE (col1 collate NOCASE asC)", NOT_NULL);
        testSingleColumnUniqueness(true, "primary key (col1 collate nocase)", NOT_NULL);
        testSingleColumnUniqueness(true, "primary KEy (col1 collate nocase DESc)", NOT_NULL);

        testSingleColumnUniqueness(false, "UNIQUE(col1, col2)", NOT_NULL);
        testSingleColumnUniqueness(false, "PRIMARY KEY(col1 , col2)", NOT_NULL);
        testSingleColumnUniqueness(false, "uniQUe  (  col1  , col2)", NOT_NULL);
        testSingleColumnUniqueness(false, "primary KEY (  col1a  )", NOT_NULL);
        testSingleColumnUniqueness(false, "UNIQUE( col1 collate NOcase, col 2)", NOT_NULL);
        testSingleColumnUniqueness(false, "UNIQUE (col1 collate NOCASE asC,col2)", NOT_NULL);
        testSingleColumnUniqueness(false, "primary key (col2,col1 collate nocase)", NOT_NULL);
        testSingleColumnUniqueness(false, "primary KEy (col2,col1 collate nocase DESc)", NOT_NULL);
    }

    private void testSingleColumnUniqueness(boolean expectSuccess, String tableConstraint, String columnConstraint) {
        testUniquenessConstraints(expectSuccess, tableConstraint, "col1", columnConstraint);
    }

    @Test
    public void testUniquenessConstraintEnforcedForMultipleUpsertKeyColumns() {
        testUniquenessConstraints(true, "UNIQUE(col1, col2)", "col1", NOT_NULL, "col2", NOT_NULL);
        testUniquenessConstraints(false, "UNIQUE(col1, col2, col3)", "col1", NOT_NULL, "col2", NOT_NULL);
        testUniquenessConstraints(true, "UNIQUE(col1, col2), UNIQUE ( col1, col2, col3 )", "col1", NOT_NULL, "col2", NOT_NULL);
        testUniquenessConstraints(false, "UNIQUE(col1), UNIQUE ( col1, col2, col3 )", "col1", NOT_NULL, "col2", NOT_NULL);

        testUniquenessConstraints(true, "PRIMARY KEY(col1, col2)", "col1", NOT_NULL, "col2", NOT_NULL);
        testUniquenessConstraints(false, "PRIMARY KEY(col1, col2, col3)", "col1", NOT_NULL, "col2", NOT_NULL);
        testUniquenessConstraints(true, "PRIMARY KEY(col1, col2), UNIQUE ( col1, col2, col3 )", "col1", NOT_NULL, "col2", NOT_NULL);
        testUniquenessConstraints(false, "PRIMARY KEY(col1), UNIQUE ( col1, col2, col3 )", "col1", NOT_NULL, "col2", NOT_NULL);

        testUniquenessConstraints(true, "uniQUE ( col1 collate   nocase ,  col2 )", "col1", NOT_NULL, "col2", NOT_NULL);
        testUniquenessConstraints(false, "unique  ( col1,col2,col3)", "col1", NOT_NULL, "col2", NOT_NULL);
        testUniquenessConstraints(true, "UNique (col1,col2) , UNIQUE ( col1, col2, col3 )", "col1", NOT_NULL, "col2", NOT_NULL);
        testUniquenessConstraints(false, "UNIQUE(col1),UNIQUE ( col1, col2, col3 )", "col1", NOT_NULL, "col2", NOT_NULL);

        testUniquenessConstraints(true, "primary KEY(col1,col2)", "col1", NOT_NULL, "col2", NOT_NULL);
        testUniquenessConstraints(false, "primary key ( col1 , col2 , col3)", "col1", NOT_NULL, "col2", NOT_NULL);
        testUniquenessConstraints(true, "primary key  ( col1  ,  col2) , UNIQUE ( col1, col2, col3 )", "col1", NOT_NULL, "col2", NOT_NULL);
        testUniquenessConstraints(false, "primARy kEy(col1), UNIQUE ( col1, col2, col3 )", "col1", NOT_NULL, "col2", NOT_NULL);
    }

    private void testUniquenessConstraints(boolean expectSuccess, String tableConstraint, String... namesAndConstraints) {
        TableModelSpecWrapper modelSpec = getMockedModelSpec(tableConstraint, namesAndConstraints);
        testUpsertVerificationWithMockedModelSpec(modelSpec, expectSuccess);
    }

    // helper methods
    private void testUpsertVerificationWithMockedModelSpec(TableModelSpecWrapper modelSpec, boolean expectSuccess) {
        UpsertPlugin plugin = new UpsertPlugin();
        assertTrue(plugin.init(modelSpec, pluginEnv));

        plugin.afterProcessVariableElements();
        verify(modelSpec, expectSuccess ? times(0) : atLeastOnce()).logError(anyString(), any(Element.class));
    }

    private TableModelSpecWrapper getMockedModelSpec(String tableConstraint, String... columnNamesAndConstraints) {
        TableModelSpecWrapper modelSpec = mock(TableModelSpecWrapper.class);
        TypeElement modelSpecElement = mock(TypeElement.class);
        List<TableModelPropertyGenerator> mockedPropertyGenerators = getMockedPropertyGenerators(columnNamesAndConstraints);
        when(modelSpec.getModelSpecElement()).thenReturn(modelSpecElement);
        when(modelSpec.getPropertyGenerators()).thenReturn(mockedPropertyGenerators);
        when(modelSpec.getTableConstraintString()).thenReturn(tableConstraint);
        return modelSpec;
    }

    private List<TableModelPropertyGenerator> getMockedPropertyGenerators(String... namesAndConstraints) {
        List<TableModelPropertyGenerator> result = new ArrayList<>();
        VariableElement mockField = mock(VariableElement.class);
        UpsertKey mockUpsertKey = mock(UpsertKey.class);
        when(mockField.getAnnotation(UpsertKey.class)).thenReturn(mockUpsertKey);
        for (int i = 0; i < namesAndConstraints.length; i += 2) {
            String name = namesAndConstraints[i];
            String constraint = namesAndConstraints[i + 1];
            TableModelPropertyGenerator mockPropertyGenerator = mock(TableModelPropertyGenerator.class);
            when(mockPropertyGenerator.getColumnName()).thenReturn(name);
            when(mockPropertyGenerator.getConstraintString()).thenReturn(constraint);
            when(mockPropertyGenerator.getField()).thenReturn(mockField);
            result.add(mockPropertyGenerator);
        }
        return result;
    }
}
