/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations;

/**
 * This annotation should not be used directly by any end-user of SquiDB. It exists only as a workaround for an error
 * logging problem inherent to the APT toolchain.
 * <p>
 * If at any point an annotation processor logs an error using the Messager object, that round of annotation processing
 * will fail and stop the build, surfacing the error to the user. This happens even if the code generation for that
 * round has run to completion. However, it will not add any code generated that round to the classpath, meaning that
 * a plethora of "cannot find symbol" errors will also be surfaced wherever the generated code is referred to, which
 * can obscure the actual error that "failed" the annotation processing.
 * <p>
 * By writing this annotation to a generated file, we can defer the logging of the error to a subsequent round of
 * annotation processing, by which point the generated class will be known to the compiler and any "cannot find symbol"
 * errors will be gone. Errors can be logged by code generation plugins using modelSpec.logError, and the
 * ErrorLoggingPlugin will write any such errors using this annotation on a dummy inner class in the generated model.
 * ErrorLoggingProcessor, a separate annotation processor, will scan for these annotations and log any such errors
 * in a later round of processing.
 * <p>
 * The outer annotation, ModelGenErrors, holds an array of the inner annotation, ModelGenError, so that multiple errors
 * can be easily logged using a single annotation on a single dummy class.
 */
public @interface ModelGenErrors {

    ModelGenError[] value();

    @interface ModelGenError {

        /**
         * The model spec class that generated this error
         */
        Class<?> specClass();

        /**
         * The name of the element that generated this error, or empty string to report the error on the root spec class
         */
        String element() default "";

        /**
         * The error message to be printed
         */
        String message();
    }
}
