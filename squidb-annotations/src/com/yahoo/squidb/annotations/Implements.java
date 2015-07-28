/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation can be used to declare that a generated model class should implement the listed interfaces. This
 * only affects the <code>implements X, Y, Z</code> declaration of the generated class--you will still have to ensure
 * that the generated class implements any required methods yourself using {@link ModelMethod}
 * <p>
 * Interfaces with no type arguments can be specified with a Class object in the {@link #interfaceClasses()} field
 * of this annotation. Interfaces that do have type arguments can use {@link #interfaceDefinitions()} with instances of
 * {@link com.yahoo.squidb.annotations.Implements.InterfaceSpec} to declare an interface class along with its type
 * arguments. At this time, model objects do not support generics, so all type arguments must be concrete classes.
 * <p>
 * Example:
 * <pre>
 *     &#064;TableModelSpec(className = "Person", tableName = "people")
 *     &#064;Implements(interfaceClasses = {Runnable.class},
 *         interfaceDefinitions = {&#064;InterfaceSpec(interfaceClass = Iterable.class, interfaceTypeArgs = Person.class)}
 *     public class PersonSpec {
 *         ...
 *     }
 * </pre>
 */
@Target(ElementType.TYPE)
public @interface Implements {

    /**
     * Helper annotation for specifying interfaces that have type arguments
     */
    @interface InterfaceSpec {

        /**
         * The class of the base interface to implement
         */
        Class<?> interfaceClass();

        /**
         * A list of type arguments that should be applied to the base interface
         */
        Class<?>[] interfaceTypeArgs() default {};

        /**
         * A list of fully qualified class name strings to be used as type arguments to the base interface. This is
         * only necessary if the type argument isn't available at compile time, e.g. if it is the generated model
         * itself. This argument will be ignored if there are any type argument classes specified using
         * {@link #interfaceTypeArgs()}
         */
        String[] interfaceTypeArgNames() default {};
    }

    /**
     * For specifying interface classes that the generated model should declare it implements
     */
    Class<?>[] interfaceClasses() default {};

    /**
     * For specifying interfaces using {@link com.yahoo.squidb.annotations.Implements.InterfaceSpec} that the generated
     * model should declare it implements
     */
    InterfaceSpec[] interfaceDefinitions() default {};
}
