/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.GenericName;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;

public class TypeConstants {

    public static final List<Modifier> PUBLIC_STATIC_FINAL = Arrays
            .asList(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
    public static final List<Modifier> PRIVATE_STATIC_FINAL = Arrays
            .asList(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

    public static final String SQUIDB_PACKAGE = "com.yahoo.squidb";
    public static final String SQUIDB_SQL_PACKAGE = SQUIDB_PACKAGE + ".sql";
    public static final String SQUIDB_DATA_PACKAGE = SQUIDB_PACKAGE + ".data";
    public static final String SQUIDB_ANDROID_DATA_PACKAGE = SQUIDB_DATA_PACKAGE + ".android";
    public static final String SQUIDB_IOS_DATA_PACKAGE = SQUIDB_DATA_PACKAGE + ".ios";
    public static final String SQUIDB_UTILITY_PACKAGE = SQUIDB_PACKAGE + ".utility";

    public static final DeclaredTypeName CREATOR = new DeclaredTypeName("android.os.Parcelable.Creator");
    public static final DeclaredTypeName CONTENT_VALUES = new DeclaredTypeName("android.content.ContentValues");

    public static final DeclaredTypeName VALUES_STORAGE = new DeclaredTypeName(SQUIDB_DATA_PACKAGE, "ValuesStorage");
    public static final DeclaredTypeName CONTENT_VALUES_STORAGE
            = new DeclaredTypeName(SQUIDB_ANDROID_DATA_PACKAGE, "ContentValuesStorage");
    public static final DeclaredTypeName HASH_MAP_VALUES_STORAGE
            = new DeclaredTypeName(SQUIDB_IOS_DATA_PACKAGE, "HashMapValuesStorage");

    public static final DeclaredTypeName ABSTRACT_MODEL = new DeclaredTypeName(SQUIDB_DATA_PACKAGE, "AbstractModel");
    public static final DeclaredTypeName TABLE_MODEL = new DeclaredTypeName(SQUIDB_DATA_PACKAGE, "TableModel");
    public static final DeclaredTypeName VIEW_MODEL = new DeclaredTypeName(SQUIDB_DATA_PACKAGE, "ViewModel");

    public static final DeclaredTypeName ANDROID_TABLE_MODEL
            = new DeclaredTypeName(SQUIDB_ANDROID_DATA_PACKAGE, "AndroidTableModel");
    public static final DeclaredTypeName ANDROID_VIEW_MODEL
            = new DeclaredTypeName(SQUIDB_ANDROID_DATA_PACKAGE, "AndroidViewModel");

    public static final DeclaredTypeName IOS_TABLE_MODEL
            = new DeclaredTypeName(SQUIDB_IOS_DATA_PACKAGE, "IOSTableModel");
    public static final DeclaredTypeName IOS_VIEW_MODEL
            = new DeclaredTypeName(SQUIDB_IOS_DATA_PACKAGE, "IOSViewModel");

    public static final DeclaredTypeName TABLE_MAPPING_VISITORS = new DeclaredTypeName(VIEW_MODEL.toString(),
            "TableMappingVisitors");
    public static final DeclaredTypeName MODEL_CREATOR = new DeclaredTypeName(SQUIDB_ANDROID_DATA_PACKAGE,
            "ModelCreator");
    public static final DeclaredTypeName SQUID_CURSOR = new DeclaredTypeName(SQUIDB_DATA_PACKAGE, "SquidCursor");
    public static final DeclaredTypeName QUERY = new DeclaredTypeName(SQUIDB_SQL_PACKAGE, "Query");
    public static final DeclaredTypeName SQL_TABLE = new DeclaredTypeName(SQUIDB_SQL_PACKAGE, "SqlTable");
    public static final DeclaredTypeName TABLE = new DeclaredTypeName(SQUIDB_SQL_PACKAGE, "Table");
    public static final DeclaredTypeName VIRTUAL_TABLE = new DeclaredTypeName(SQUIDB_SQL_PACKAGE, "VirtualTable");
    public static final DeclaredTypeName VIEW = new DeclaredTypeName(SQUIDB_SQL_PACKAGE, "View");
    public static final DeclaredTypeName SUBQUERY_TABLE = new DeclaredTypeName(SQUIDB_SQL_PACKAGE, "SubqueryTable");

    public static final DeclaredTypeName BYTE_ARRAY;

    static {
        BYTE_ARRAY = CoreTypes.PRIMITIVE_BYTE.clone();
        BYTE_ARRAY.setArrayDepth(1);
    }

    public static final DeclaredTypeName PROPERTY = new DeclaredTypeName(SQUIDB_SQL_PACKAGE, "Property");
    public static final DeclaredTypeName LONG_PROPERTY = new DeclaredTypeName(PROPERTY.toString(), "LongProperty");
    public static final DeclaredTypeName INTEGER_PROPERTY = new DeclaredTypeName(PROPERTY.toString(),
            "IntegerProperty");
    public static final DeclaredTypeName DOUBLE_PROPERTY = new DeclaredTypeName(PROPERTY.toString(), "DoubleProperty");
    public static final DeclaredTypeName STRING_PROPERTY = new DeclaredTypeName(PROPERTY.toString(), "StringProperty");
    public static final DeclaredTypeName BOOLEAN_PROPERTY = new DeclaredTypeName(PROPERTY.toString(),
            "BooleanProperty");
    public static final DeclaredTypeName BLOB_PROPERTY = new DeclaredTypeName(PROPERTY.toString(), "BlobProperty");

    public static final DeclaredTypeName PROPERTY_ARRAY;
    public static final DeclaredTypeName PROPERTY_VARARGS;

    static {
        PROPERTY.setTypeArgs(Collections.singletonList(GenericName.DEFAULT_WILDCARD));

        PROPERTY_ARRAY = PROPERTY.clone();
        PROPERTY_ARRAY.setArrayDepth(1);

        PROPERTY_VARARGS = PROPERTY.clone();
        PROPERTY_VARARGS.setArrayDepth(1);
        PROPERTY_VARARGS.setIsVarArgs(true);
    }

    private static final Set<DeclaredTypeName> PROPERTY_TYPES = new HashSet<DeclaredTypeName>();

    static {
        PROPERTY_TYPES.add(TypeConstants.BLOB_PROPERTY);
        PROPERTY_TYPES.add(TypeConstants.BOOLEAN_PROPERTY);
        PROPERTY_TYPES.add(TypeConstants.DOUBLE_PROPERTY);
        PROPERTY_TYPES.add(TypeConstants.INTEGER_PROPERTY);
        PROPERTY_TYPES.add(TypeConstants.LONG_PROPERTY);
        PROPERTY_TYPES.add(TypeConstants.STRING_PROPERTY);
    }

    public static boolean isPropertyType(DeclaredTypeName type) {
        return PROPERTY_TYPES.contains(type);
    }
}
