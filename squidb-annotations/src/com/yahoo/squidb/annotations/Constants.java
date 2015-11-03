/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * By default, public static final fields in model spec classes are copied to the generated model classes as constants.
 * However, if these constants reference fields in the generated model (e.g. properties), there may be issues with the
 * order of class loading if the generated models refer back to the model spec before the models themselves are fully
 * initialized. To work around this issue, constants may be declared in a public static inner class in the model
 * spec and annotated with @Constants. This inner class will not be loaded until all generated model fields are
 * initialized.
 * <p>
 * Any constants that need to refer to the model schema (particularly in a view model) should be declared
 * in a static inner class as in this example:
 * <pre>
 *     &#064;ViewModelSpec(className = "PersonViewModel", viewName = "people")
 *     public class PersonViewSpecSpec {
 *
 *          public static final StringProperty NAME = Person.NAME;
 *
 *          &#064;Constants
 *          public static class Const {
 *              public static final Order DEFAULT_ORDER = PersonViewModel.NAME.asc();
 *          }
 *     }
 * </pre>
 */
@Target(ElementType.TYPE)
public @interface Constants {
}
