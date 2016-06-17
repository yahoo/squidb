/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.squidb.sql.Field;
import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.utility.VersionCode;

/**
 * Declarations of SQLite functions found in <a href="http://sqlite.org/json1.html">the json1 extension</a>. Note that
 * these functions will only work if you are connecting to a version of SQLite >= 3.9.0 with the json1 extension
 * enabled.
 * <br/>
 * From the SQLite json1 documentation:
 * <br/>
 * For functions that accept JSON as their first argument, that argument can be a JSON object, array, number, string,
 * or null. SQLite numeric values and NULL values are interpreted as JSON numbers and nulls, respectively. SQLite text
 * values can be understood as JSON objects, arrays, or strings. If an SQLite text value that is not a well-formed JSON
 * object, array, or string is passed into json1 function, that function will usually throw an error. (An exception is
 * the json_valid(X) function which returns 1 if X is well-formed JSON and 0 if it is not.)
 * <br/>
 * For functions that accept PATH arguments, that PATH must be well-formed or else the function will throw an error.
 * A well-formed PATH is a text value that begins with exactly one '$' character followed by zero or more instances of
 * ".objectlabel" or "[arrayindex]".
 * <br/>
 * For functions that accept "value" arguments (e.g. {@link #jsonInsert},{@link #jsonReplace}, or {@link #jsonSet}),
 * those arguments is usually understood to be a literal strings that are quoted and becomes JSON string values in the
 * result. Even if the input value strings look like well-formed JSON, they are still interpreted as literal strings in
 * the result. However, if a value argument come directly from the result of another json1 function, then the argument
 * is understood to be actual JSON and the complete JSON is inserted rather than a quoted string.
 *
 * @see <a href="http://sqlite.org/json1.html">the json1 extension</a>
 */
public class JSONFunctions {

    public static final VersionCode JSON1_MIN_VERSION = new VersionCode(3, 9, 0, 0);
    public static final VersionCode JSON1_GROUP_FUNCTIONS_VERSION = new VersionCode(3, 10, 0, 0);

    /**
     * Wrapper for the json() SQL function
     *
     * @see <a href="http://sqlite.org/json1.html#jmini">The json() function</a>
     */
    public static Function<String> json(Object arg) {
        return new JSONArgumentFunction<>("json", arg);
    }

    /**
     * Wrapper for the json_array() SQL function
     *
     * @see <a href="http://sqlite.org/json1.html#jarray">The json_array() function</a>
     */
    public static Function<String> jsonArray(Object... args) {
        return new JSONArgumentFunction<>("json_array", null, args);
    }

    /**
     * Wrapper for the json_array_length() SQL function
     *
     * @param json a JSON argument as described in the {@link JSONFunctions} documentation
     * @see <a href="http://sqlite.org/json1.html#jarraylen">The json_array_length() function</a>
     */
    public static Function<Integer> jsonArrayLength(Object json) {
        return new JSONArgumentFunction<>("json_array_length", json);
    }

    /**
     * Wrapper for the json_array_length() SQL function
     *
     * @param json a JSON argument as described in the {@link JSONFunctions} documentation
     * @param path a PATH argument as described in the {@link JSONFunctions} documentation
     * @see <a href="http://sqlite.org/json1.html#jarraylen">The json_array_length() function</a>
     */
    public static Function<Integer> jsonArrayLength(Object json, String path) {
        return new JSONArgumentFunction<>("json_array_length", json, path);
    }

    /**
     * Wrapper for the json_extract() SQL function
     *
     * @param json a JSON argument as described in the {@link JSONFunctions} documentation
     * @param path a PATH argument as described in the {@link JSONFunctions} documentation
     * @see <a href="http://sqlite.org/json1.html#jex">The json_extract() function</a>
     */
    public static <T> Function<T> jsonExtract(Object json, String path) {
        return new JSONArgumentFunction<>("json_extract", json, path);
    }

    /**
     * Wrapper for the json_extract() SQL function
     *
     * @param json a JSON argument as described in the {@link JSONFunctions} documentation
     * @param paths 1 or more PATH arguments as described in the {@link JSONFunctions} documentation
     * @see <a href="http://sqlite.org/json1.html#jex">The json_extract() function</a>
     */
    public static Function<String> jsonExtract(Object json, String... paths) {
        return new JSONArgumentFunction<>("json_extract", json, (Object[]) paths);
    }

    /**
     * Wrapper for the json_type() SQL function
     *
     * @param json a JSON argument as described in the {@link JSONFunctions} documentation
     * @see <a href="http://sqlite.org/json1.html#jtype">The json_type() function</a>
     */
    public static Function<String> jsonType(Object json) {
        return new JSONArgumentFunction<>("json_type", json);
    }

    /**
     * Wrapper for the json_type() SQL function
     *
     * @param json a JSON argument as described in the {@link JSONFunctions} documentation
     * @param path a PATH argument as described in the {@link JSONFunctions} documentation
     * @see <a href="http://sqlite.org/json1.html#jtype">The json_type() function</a>
     */
    public static Function<String> jsonType(Object json, String path) {
        return new JSONArgumentFunction<>("json_type", json, path);
    }

    /**
     * Wrapper for the json_insert() SQL function
     *
     * @param json a JSON argument as described in the {@link JSONFunctions} documentation
     * @param pathValuePairs an alternating sequence of PATH arguments and VALUES to insert for those paths
     * (path1, value1, path2, value2, ...)
     * @see <a href="http://sqlite.org/json1.html#jins">The json_insert() function</a>
     */
    public static Function<String> jsonInsert(Object json, Object... pathValuePairs) {
        return new JSONArgumentFunction<>("json_insert", json, pathValuePairs);
    }

    /**
     * Wrapper for the json_replace() SQL function
     *
     * @param json a JSON argument as described in the {@link JSONFunctions} documentation
     * @param pathValuePairs an alternating sequence of PATH arguments and VALUES to replace for those paths
     * (path1, value1, path2, value2, ...)
     * @see <a href="http://sqlite.org/json1.html#jrepl">The json_replace() function</a>
     */
    public static Function<String> jsonReplace(Object json, Object... pathValuePairs) {
        return new JSONArgumentFunction<>("json_replace", json, pathValuePairs);
    }

    /**
     * Wrapper for the json_set() SQL function
     *
     * @param json a JSON argument as described in the {@link JSONFunctions} documentation
     * @param pathValuePairs an alternating sequence of PATH arguments and VALUES to replace for those paths
     * (path1, value1, path2, value2, ...)
     * @see <a href="http://sqlite.org/json1.html#jset">The json_set() function</a>
     */
    public static Function<String> jsonSet(Object json, Object... pathValuePairs) {
        return new JSONArgumentFunction<>("json_set", json, pathValuePairs);
    }

    /**
     * Wrapper for the json_remove() SQL function
     *
     * @param json a JSON argument as described in the {@link JSONFunctions} documentation
     * @param paths a list of PATH arguments as described in the {@link JSONFunctions} documentation
     * @see <a href = "http://sqlite.org/json1.html#jrm">the json_remove() function</a>
     */
    public static Function<String> jsonRemove(Object json, Object... paths) {
        return new JSONArgumentFunction<>("json_remove", json, paths);
    }

    /**
     * Wrapper for the json_object() SQL function
     *
     * @param labelValuePairs an alternating sequence of labels and values to construct a JSON object from
     * @see <a href = "http://sqlite.org/json1.html#jobj">the json_object() function</a>
     */
    public static Function<String> jsonObject(Object... labelValuePairs) {
        return new JSONArgumentFunction<>("json_object", null, labelValuePairs);
    }

    /**
     * Wrapper for the json_valid() SQL function
     *
     * @see <a href="http://sqlite.org/json1.html#jvalid">The json_valid() function</a>
     */
    public static Function<Integer> jsonValid(Object arg) {
        return new JSONArgumentFunction<>("json_valid", arg);
    }

    /**
     * Wrapper for the json_group_array() SQL aggregate function. This function requires SQLite version 3.10.0 or
     * higher.
     *
     * @see <a href="http://sqlite.org/json1.html#jgrouparray">The json_group_array() function</a>
     */
    public static Function<String> jsonGroupArray(Field<?> arg) {
        return new JSONArgumentFunction<>(JSON1_GROUP_FUNCTIONS_VERSION, "json_group_array", arg);
    }

    /**
     * Wrapper for the json_group_object() SQL aggregate function. This function requires SQLite version 3.10.0 or
     * higher.
     *
     * @see <a href="http://sqlite.org/json1.html#jgroupobject">The json_group_object() function</a>
     */
    public static Function<String> jsonGroupObject(Field<?> keys, Field<?> values) {
        return new JSONArgumentFunction<>(JSON1_GROUP_FUNCTIONS_VERSION, "json_group_object", keys, values);
    }
}
