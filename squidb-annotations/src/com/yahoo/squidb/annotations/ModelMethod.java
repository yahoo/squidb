/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Public static methods in model specs annotated with &#064;ModelMethod will be added to the model definition.
 * Model methods should take as their first parameter an instance of the model:
 * <pre>
 *     &#064;TableModelSpec(className = "Person", tableName = "people")
 *     public class PersonSpec {
 *
 *          String firstName;
 *
 *          String lastName;
 *
 *          &#064;ModelMethod
 *          public static String getFullName(Person instance) {
 *              return instance.getFirstName() + " " + instance.getLastName();
 *          }
 *     }
 * </pre>
 *
 * The generated Person class will declare a method <code>getFullName()</code> that takes no arguments and calls back
 * to the static method defined in the spec for its implementation
 */
@Target(value = ElementType.METHOD)
public @interface ModelMethod {

    String name() default "";

}
