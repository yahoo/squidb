/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicIntegerPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicLongPropertyGenerator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

public class TypeConstants {

    public static boolean isConstant(VariableElement field) {
        Set<Modifier> modifiers = field.getModifiers();
        return modifiers != null && modifiers.containsAll(Arrays.asList(Modifier.STATIC, Modifier.FINAL));
    }

    public static boolean isVisibleConstant(VariableElement field) {
        return isConstant(field) && !field.getModifiers().contains(Modifier.PRIVATE);
    }

    public static final String SQUIDB_PACKAGE = "com.yahoo.squidb";
    public static final String SQUIDB_SQL_PACKAGE = SQUIDB_PACKAGE + ".sql";
    public static final String SQUIDB_DATA_PACKAGE = SQUIDB_PACKAGE + ".data";
    public static final String SQUIDB_ANDROID_PACKAGE = SQUIDB_PACKAGE + ".android";
    public static final String SQUIDB_UTILITY_PACKAGE = SQUIDB_PACKAGE + ".utility";

    public static final ClassName CREATOR = ClassName.get("android.os", "Parcelable", "Creator");

    public static final ClassName VALUES_STORAGE = ClassName.get(SQUIDB_DATA_PACKAGE, "ValuesStorage");
    public static final ClassName MAP_VALUES_STORAGE = ClassName.get(SQUIDB_DATA_PACKAGE, "MapValuesStorage");
    public static final ClassName CONTENT_VALUES = ClassName.get("android.content", "ContentValues");
    public static final ClassName MAP = ClassName.get("java.util", "Map");
    public static final TypeName MAP_VALUES = ParameterizedTypeName.get(MAP,
            ClassName.get(String.class), ClassName.OBJECT);

    public static final ClassName ABSTRACT_MODEL = ClassName.get(SQUIDB_DATA_PACKAGE, "AbstractModel");
    public static final ClassName TABLE_MODEL = ClassName.get(SQUIDB_DATA_PACKAGE, "TableModel");
    public static final ClassName VIEW_MODEL = ClassName.get(SQUIDB_DATA_PACKAGE, "ViewModel");

    public static final ClassName ANDROID_TABLE_MODEL = ClassName.get(SQUIDB_ANDROID_PACKAGE, "AndroidTableModel");
    public static final ClassName ANDROID_VIEW_MODEL = ClassName.get(SQUIDB_ANDROID_PACKAGE, "AndroidViewModel");

    public static final ClassName TABLE_MAPPING_VISITORS = ClassName.get(SQUIDB_DATA_PACKAGE, "ViewModel", "TableMappingVisitors");
    public static final ClassName MODEL_CREATOR = ClassName.get(SQUIDB_ANDROID_PACKAGE, "ModelCreator");
    public static final ClassName SQUID_CURSOR = ClassName.get(SQUIDB_DATA_PACKAGE, "SquidCursor");
    public static final ClassName QUERY = ClassName.get(SQUIDB_SQL_PACKAGE, "Query");
    public static final ClassName SQL_TABLE = ClassName.get(SQUIDB_SQL_PACKAGE, "SqlTable");
    public static final ClassName TABLE = ClassName.get(SQUIDB_SQL_PACKAGE, "Table");
    public static final ClassName VIRTUAL_TABLE = ClassName.get(SQUIDB_SQL_PACKAGE, "VirtualTable");
    public static final ClassName VIEW = ClassName.get(SQUIDB_SQL_PACKAGE, "View");
    public static final ClassName SUBQUERY_TABLE = ClassName.get(SQUIDB_SQL_PACKAGE, "SubqueryTable");
    public static final ClassName TABLE_MODEL_NAME = ClassName.get(SQUIDB_SQL_PACKAGE, "TableModelName");

    public static final TypeName BYTE_ARRAY = ArrayTypeName.of(TypeName.BYTE);

    public static final ClassName PROPERTY_UNPARAMETERIZED = ClassName.get(SQUIDB_SQL_PACKAGE, "Property");
    public static final TypeName PROPERTY = ParameterizedTypeName.get(ClassName.get(SQUIDB_SQL_PACKAGE, "Property"),
            WildcardTypeName.subtypeOf(Object.class));
    public static final ClassName LONG_PROPERTY = ClassName.get(SQUIDB_SQL_PACKAGE, "Property", "LongProperty");
    public static final ClassName INTEGER_PROPERTY = ClassName.get(SQUIDB_SQL_PACKAGE, "Property", "IntegerProperty");
    public static final ClassName DOUBLE_PROPERTY = ClassName.get(SQUIDB_SQL_PACKAGE, "Property", "DoubleProperty");
    public static final ClassName STRING_PROPERTY = ClassName.get(SQUIDB_SQL_PACKAGE, "Property", "StringProperty");
    public static final ClassName BOOLEAN_PROPERTY = ClassName.get(SQUIDB_SQL_PACKAGE, "Property", "BooleanProperty");
    public static final ClassName BLOB_PROPERTY = ClassName.get(SQUIDB_SQL_PACKAGE, "Property", "BlobProperty");
    public static final ClassName ENUM_PROPERTY = ClassName.get(SQUIDB_SQL_PACKAGE, "Property", "EnumProperty");

    public static final TypeName PROPERTY_ARRAY = ArrayTypeName.of(PROPERTY);

    public static final ClassName OBJECTIVE_C_NAME = ClassName.get("com.google.j2objc.annotations", "ObjectiveCName");

    private static final Set<TypeName> BASIC_PROPERTY_TYPES = new HashSet<>();

    static {
        BASIC_PROPERTY_TYPES.add(BLOB_PROPERTY);
        BASIC_PROPERTY_TYPES.add(BOOLEAN_PROPERTY);
        BASIC_PROPERTY_TYPES.add(DOUBLE_PROPERTY);
        BASIC_PROPERTY_TYPES.add(INTEGER_PROPERTY);
        BASIC_PROPERTY_TYPES.add(LONG_PROPERTY);
        BASIC_PROPERTY_TYPES.add(STRING_PROPERTY);
    }

    public static boolean isBasicPropertyType(TypeName type) {
        return BASIC_PROPERTY_TYPES.contains(type);
    }

    public static boolean isIntegerType(TypeName type) {
        return BasicIntegerPropertyGenerator.handledColumnTypes().contains(type) ||
                BasicLongPropertyGenerator.handledColumnTypes().contains(type);
    }

    public static boolean isGenericType(TypeName type) {
        return type instanceof WildcardTypeName || type instanceof TypeVariableName;
    }
}
