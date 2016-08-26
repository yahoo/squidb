/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.json.JSONFunctions;
import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Thing;
import com.yahoo.squidb.utility.Logger;
import com.yahoo.squidb.utility.VersionCode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JSONFunctionTest extends DatabaseTestCase {

    private void testJsonFunction(Runnable toTest) {
        testJsonFunction(toTest, JSONFunctions.JSON1_MIN_VERSION);
    }

    private void testJsonFunction(Runnable toTest, VersionCode minVersionCode) {
        if (isJson1ExtensionEnabled()) {
            testForMinVersionCode(minVersionCode, toTest);
        }
    }

    private boolean isJson1ExtensionEnabled() {
        try {
            return database.simpleQueryForLong(Query.select(JSONFunctions.jsonValid("{ \"a\" : \"b\" }"))) != 0;
        } catch (RuntimeException e) {
            Logger.d("JSONFunctionTest", "JSON1 extension not available", e);
            return false;
        }
    }

    public void testJson() {
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                Function<String> json = JSONFunctions.json(" { \"this\" : \"is\", \"a\": [ \"test\" ] } ");
                String minified = database.simpleQueryForString(Query.select(json));
                assertEquals("{\"this\":\"is\",\"a\":[\"test\"]}", minified);
            }
        });
    }

    public void testMalformedJsonThrowsError() {
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                testThrowsRuntimeException(new Runnable() {
                    @Override
                    public void run() {
                        Function<String> json = JSONFunctions.json(" { \"this\" : \"is\", \"a\": [ \"test\"  } ");
                        String minified = database.simpleQueryForString(Query.select(json));
                        assertNull(minified);
                    }
                });
            }
        });
    }

    public void testJsonArray() {
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                Function<String> jsonArray = JSONFunctions.jsonArray(1, 1, 2, 3, 5, 8, 13);
                String result = database.simpleQueryForString(Query.select(jsonArray));
                assertEquals("[1,1,2,3,5,8,13]", result);
            }
        });
    }

    public void testJsonArrayLength() {
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                Function<Integer> jsonArrayLength = JSONFunctions.jsonArrayLength(
                        JSONFunctions.jsonArray(1, 1, 2, 3, 5, 8, 13));
                long result = database.simpleQueryForLong(Query.select(jsonArrayLength));
                assertEquals(7, result);
            }
        });
    }

    public void testJsonArrayLengthWithPath() {
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                Function<String> json = JSONFunctions.json(" { \"this\" : \"is\", \"a\": [ \"test\" ] } ");
                Function<Integer> jsonArrayLength = JSONFunctions.jsonArrayLength(json, "$.a");
                long result = database.simpleQueryForLong(Query.select(jsonArrayLength));
                assertEquals(1, result);
            }
        });
    }

    public void testJsonExtractSinglePath() {
        // These tests are taken entirely from the examples at http://sqlite.org/json1.html#jex
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                testJsonExtractSinglePathInternal("{\"a\":2,\"c\":[4,5,{\"f\":7}]}", "$",
                        "{\"a\":2,\"c\":[4,5,{\"f\":7}]}");
                testJsonExtractSinglePathInternal("{\"a\":2,\"c\":[4,5,{\"f\":7}]}", "$.c", "[4,5,{\"f\":7}]");
                testJsonExtractSinglePathInternal("{\"a\":2,\"c\":[4,5,{\"f\":7}]}", "$.c[2]", "{\"f\":7}");
                testJsonExtractSinglePathInternal("{\"a\":2,\"c\":[4,5,{\"f\":7}]}", "$.c[2].f", 7);
                testJsonExtractSinglePathInternal("{\"a\":2,\"c\":[4,5,{\"f\":7}]}", "$.x", null);
            }
        });
    }

    private <T> void testJsonExtractSinglePathInternal(String json, String path, T expectedValue) {
        Function<T> extract = JSONFunctions.jsonExtract(json, path);
        Query sql = Query.select(extract);
        Object value;
        if (expectedValue instanceof String || expectedValue == null) {
            value = database.simpleQueryForString(sql);
            assertEquals(expectedValue, value);
        } else if (expectedValue instanceof Number) {
            value = database.simpleQueryForLong(sql);
            assertEquals(((Number) expectedValue).longValue(), value);
        } else {
            fail("Invalid expected value");
        }
    }

    public void testJsonExtractMultiplePaths() {
        // These tests are taken entirely from the examples at http://sqlite.org/json1.html#jex
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                testJsonExtractMultiplePathsInternal("{\"a\":2,\"c\":[4,5],\"f\":7}", "[[4,5],2]", "$.c", "$.a");
                testJsonExtractMultiplePathsInternal("{\"a\":2,\"c\":[4,5,{\"f\":7}]}", "[null,2]", "$.x", "$.a");
            }
        });
    }

    private void testJsonExtractMultiplePathsInternal(String json, String expectedValue, String... paths) {
        Function<String> extract = JSONFunctions.jsonExtract(json, paths);
        String value = database.simpleQueryForString(Query.select(extract));
        assertEquals(expectedValue, value);
    }

    public void testJsonType() {
        // These tests are taken entirely from the examples at http://sqlite.org/json1.html#jtype
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                String jsonString = "{\"a\":[2,3.5,true,false,null,\"x\"]}";
                testJsonTypeInternal(jsonString, null, "object");
                testJsonTypeInternal(jsonString, "$", "object");
                testJsonTypeInternal(jsonString, "$.a", "array");
                testJsonTypeInternal(jsonString, "$.a[0]", "integer");
                testJsonTypeInternal(jsonString, "$.a[1]", "real");
                testJsonTypeInternal(jsonString, "$.a[2]", "true");
                testJsonTypeInternal(jsonString, "$.a[3]", "false");
                testJsonTypeInternal(jsonString, "$.a[4]", "null");
                testJsonTypeInternal(jsonString, "$.a[5]", "text");
                testJsonTypeInternal(jsonString, "$.a[6]", null);
            }
        });
    }

    private void testJsonTypeInternal(String json, String path, String expectedType) {
        Function<String> type;
        if (path == null) {
            type = JSONFunctions.jsonType(json);
        } else {
            type = JSONFunctions.jsonType(json, path);
        }
        String result = database.simpleQueryForString(Query.select(type));
        assertEquals(expectedType, result);
    }

    public void testJsonInsert() {
        // These tests are taken entirely from the examples at http://sqlite.org/json1.html#jins
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                testJsonInsertReplaceSetInternal(0, "$.a", 99, "{\"a\":2,\"c\":4}");
                testJsonInsertReplaceSetInternal(0, "$.e", 99, "{\"a\":2,\"c\":4,\"e\":99}");
            }
        });
    }

    public void testJsonReplace() {
        // These tests are taken entirely from the examples at http://sqlite.org/json1.html#jrepl
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                testJsonInsertReplaceSetInternal(1, "$.a", 99, "{\"a\":99,\"c\":4}");
                testJsonInsertReplaceSetInternal(1, "$.e", 99, "{\"a\":2,\"c\":4}");
            }
        });
    }

    public void testJsonSet() {
        // These tests are taken entirely from the examples at http://sqlite.org/json1.html#jset
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                testJsonInsertReplaceSetInternal(2, "$.a", 99, "{\"a\":99,\"c\":4}");
                testJsonInsertReplaceSetInternal(2, "$.e", 99, "{\"a\":2,\"c\":4,\"e\":99}");
                testJsonInsertReplaceSetInternal(2, "$.c", "[97,96]", "{\"a\":2,\"c\":\"[97,96]\"}");
                testJsonInsertReplaceSetInternal(2, "$.c", JSONFunctions.json("[97,96]"),
                        "{\"a\":2,\"c\":[97,96]}");
                testJsonInsertReplaceSetInternal(2, "$.c", JSONFunctions.jsonArray(97, 96),
                        "{\"a\":2,\"c\":[97,96]}");
            }
        });
    }

    // 0 for insert, 1 for replace, 2 for set
    private void testJsonInsertReplaceSetInternal(int type, String path, Object value, String expectedResult) {
        String jsonString = "{\"a\":2,\"c\":4}";
        Function<String> func = null;
        switch (type) {
            case 0:
                func = JSONFunctions.jsonInsert(jsonString, path, value);
                break;
            case 1:
                func = JSONFunctions.jsonReplace(jsonString, path, value);
                break;
            case 2:
                func = JSONFunctions.jsonSet(jsonString, path, value);
                break;
            default:
                fail("Unsupported insert/replace/set type " + type);
        }
        String result = database.simpleQueryForString(Query.select(func));
        assertEquals(expectedResult, result);
    }

    public void testJsonRemove() {
        // These tests are taken entirely from the examples at http://sqlite.org/json1.html#jrm
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                String jsonArray = "[0,1,2,3,4]";
                testJsonRemoveInternal(jsonArray, "[0,1,3,4]", "$[2]");
                testJsonRemoveInternal(jsonArray, "[1,3,4]", "$[2]", "$[0]");
                testJsonRemoveInternal(jsonArray, "[1,2,4]", "$[0]", "$[2]");

                String jsonObject = "{\"x\":25,\"y\":42}";
                testJsonRemoveInternal(jsonObject, "{\"x\":25,\"y\":42}");
                testJsonRemoveInternal(jsonObject, "{\"x\":25,\"y\":42}", "$.z");
                testJsonRemoveInternal(jsonObject, "{\"x\":25}", "$.y");
                testJsonRemoveInternal(jsonObject, null, "$");
            }
        });
    }

    private void testJsonRemoveInternal(String jsonString, String expectedResult, String... paths) {
        Function<String> jsonRemove = JSONFunctions.jsonRemove(jsonString, (Object[]) paths);
        String result = database.simpleQueryForString(Query.select(jsonRemove));
        assertEquals(expectedResult, result);
    }

    public void testJsonObject() {
        // These tests are taken entirely from the examples at http://sqlite.org/json1.html#jobj
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                testJsonObjectInternal("{\"a\":2,\"c\":4}", "a", 2, "c", 4);
                testJsonObjectInternal("{\"a\":2,\"c\":\"{e:5}\"}", "a", 2, "c", "{e:5}");
                testJsonObjectInternal("{\"a\":2,\"c\":{\"e\":5}}", "a", 2, "c", JSONFunctions.jsonObject("e", 5));
            }
        });
    }

    private void testJsonObjectInternal(String expectedResult, Object... args) {
        Function<String> jsonObject = JSONFunctions.jsonObject(args);
        String result = database.simpleQueryForString(Query.select(jsonObject));
        assertEquals(expectedResult, result);
    }

    public void testJsonValid() {
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                Function<Integer> valid = JSONFunctions.jsonValid(" { \"this\" : \"is\", \"a\": [ \"test\" ] } ");
                Function<Integer> invalid = JSONFunctions.jsonValid(" { \"this\" : \"is\", \"a\": [ \"test\"  } ");

                long validResult = database.simpleQueryForLong(Query.select(valid));
                assertEquals(1, validResult);

                long invalidResult = database.simpleQueryForLong(Query.select(invalid));
                assertEquals(0, invalidResult);
            }
        });
    }

    public void testJsonGroupArray() {
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                Thing thing = new Thing();
                for (int i = 0; i < 5; i++) {
                    thing.setFoo(Integer.toString(i));
                    database.createNew(thing);
                }
                Function<String> groupArray = JSONFunctions.jsonGroupArray(Thing.FOO);
                String result = database.simpleQueryForString(Query.select(groupArray).from(Thing.TABLE));
                try {
                    JSONArray resultArray = new JSONArray(result);
                    Set<String> resultValues = new HashSet<>();
                    for (int i = 0; i < resultArray.length(); i++) {
                        resultValues.add(resultArray.getString(i));
                    }
                    assertEquals(5, resultValues.size());
                    assertTrue(resultValues.containsAll(Arrays.asList("0", "1", "2", "3", "4")));
                } catch (JSONException e) {
                    fail("JSONException: " + e.getMessage());
                }
            }
        }, JSONFunctions.JSON1_GROUP_FUNCTIONS_VERSION);
    }

    public void testJsonGroupObject() {
        testJsonFunction(new Runnable() {
            @Override
            public void run() {
                Thing thing = new Thing();
                for (int i = 0; i < 5; i++) {
                    thing.setFoo(Integer.toString(i))
                            .setBar(i * 2);
                    database.createNew(thing);
                }
                Function<String> groupObject = JSONFunctions.jsonGroupObject(Thing.FOO, Thing.BAR);
                String result = database.simpleQueryForString(Query.select(groupObject).from(Thing.TABLE));
                try {
                    JSONObject resultObject = new JSONObject(result);
                    assertEquals(5, resultObject.length());
                    for (int i = 0; i < 5; i++) {
                        assertEquals(i * 2, resultObject.getInt(Integer.toString(i)));
                    }
                } catch (JSONException e) {
                    fail("JSONException: " + e.getMessage());
                }
            }
        }, JSONFunctions.JSON1_GROUP_FUNCTIONS_VERSION);
    }
}
