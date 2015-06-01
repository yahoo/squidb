/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * A model spec for generating a model that subclasses from another model. Properties can be re-declared with new names
 * in an inherited model. For example:
 * <pre>
 *     &#064;TableModelSpec(className = "Metadata", tableName = "metadata")
 *     public class MetadataSpec {
 *
 *          String data1;
 *
 *          String data2;
 *
 *     }
 *
 *     ...
 *
 *     &#064;InheritedModelSpec(className = "KeyValueMetadata", inheritsFrom = "com.mypackage.Metadata")
 *     public class SpecificMetadataSpec {
 *
 *         public static final StringProperty KEY = Metadata.DATA_1;
 *
 *         public static final StringProperty VALUE = Metadata.DATA_2;
 *
 *     }
 *
 * </pre>
 *
 * SpecificMetadata will have getters and setters for KEY and VALUE, which are really just DATA_1 and DATA_2 accessed
 * by a more readable name.
 */
@Target(ElementType.TYPE)
public @interface InheritedModelSpec {

    /**
     * The name of the class to be generated
     */
    String className();

    /**
     * Fully qualified class name of model class to inherit from. When specified, the object is processed similar to
     * ViewModel but extends from the specified class.
     */
    String inheritsFrom();

}
