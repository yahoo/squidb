/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CompiledArgumentResolver {

    private static final Pattern REPLACEABLE_ARRAY_PARAM_PATTERN =
            Pattern.compile(SqlStatement.REPLACEABLE_ARRAY_PARAMETER_REGEX);

    private final String compiledSql;
    private final List<Object> argsOrReferences;

    private final boolean hasCollectionArgs;

    private boolean hasImmutableArgs = false;
    private List<Collection<?>> collectionArgs = new ArrayList<Collection<?>>();

    private static final int CACHE_SIZE = 5;
    private SimpleLruCache<String, String> compiledSqlCache;
    private SimpleLruCache<String, Object[]> argArrayCache;

    private Object[] compiledArgs = null;

    public CompiledArgumentResolver(String compiledSql, List<Object> argsOrReferences) {
        this.compiledSql = compiledSql;
        this.argsOrReferences = argsOrReferences;
        this.hasCollectionArgs = compiledSql.contains(SqlStatement.REPLACEABLE_ARRAY_PARAMETER);
        if (hasCollectionArgs) {
            findCollectionArgs();
            compiledSqlCache = new SimpleLruCache<String, String>(CACHE_SIZE);
            argArrayCache = new SimpleLruCache<String, Object[]>(CACHE_SIZE);
        }
    }

    private void findCollectionArgs() {
        for (Object arg : argsOrReferences) {
            if (arg instanceof Collection<?>) {
                collectionArgs.add((Collection<?>) arg);
            }
        }
    }

    public CompiledStatement resolveToCompiledStatement() {
        String cacheKey = hasCollectionArgs ? getCacheKey() : null;
        int totalArgSize = calculateArgsSizeWithCollectionArgs();
        boolean largeArgMode = totalArgSize > SqlStatement.MAX_VARIABLE_NUMBER;
        return new CompiledStatement(resolveSqlString(cacheKey, largeArgMode),
                resolveSqlArguments(cacheKey, totalArgSize, largeArgMode));
    }

    private String getCacheKey() {
        StringBuilder cacheKey = new StringBuilder();
        for (Collection<?> collection : collectionArgs) {
            cacheKey.append(collection.size()).append(":");
        }
        return cacheKey.toString();
    }

    private String resolveSqlString(String cacheKey, boolean largeArgMode) {
        if (hasCollectionArgs) {
            if (!largeArgMode) {
                String cachedResult = compiledSqlCache.get(cacheKey);
                if (cachedResult != null) {
                    return cachedResult;
                }
            }

            StringBuilder result = new StringBuilder(compiledSql.length());
            Matcher m = REPLACEABLE_ARRAY_PARAM_PATTERN.matcher(compiledSql);
            int index = 0;
            int lastStringIndex = 0;
            while (m.find()) {
                result.append(compiledSql.substring(lastStringIndex, m.start()));
                Collection<?> values = collectionArgs.get(index);
                if (largeArgMode) {
                    SqlUtils.addInlineCollectionToSqlString(result, values);
                } else {
                    appendCollectionVariableStringForSize(result, values.size());
                }
                lastStringIndex = m.end();
                index++;
            }
            result.append(compiledSql.substring(lastStringIndex, compiledSql.length()));

            String resultSql = result.toString();
            if (!largeArgMode) {
                compiledSqlCache.put(cacheKey, resultSql);
            }
            return resultSql;
        } else {
            return compiledSql;
        }
    }

    private void appendCollectionVariableStringForSize(StringBuilder builder, int size) {
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(SqlStatement.REPLACEABLE_PARAMETER);
        }
    }

    private Object[] resolveSqlArguments(String cacheKey, int totalArgSize, boolean largeArgMode) {
        if (!hasImmutableArgs) {
            if (hasCollectionArgs) {
                Object[] cachedResult = argArrayCache.get(cacheKey);
                if (cachedResult == null) {
                    int size = largeArgMode ? calculateArgsSizeWithoutCollectionArgs() : totalArgSize;
                    if (compiledArgs == null || compiledArgs.length != size) {
                        cachedResult = new Object[size];
                    } else {
                        cachedResult = compiledArgs;
                    }
                    argArrayCache.put(cacheKey, cachedResult);
                }
                compiledArgs = cachedResult;
            } else {
                if (compiledArgs == null) {
                    compiledArgs = new Object[argsOrReferences.size()];
                }
            }
            populateCompiledArgs(largeArgMode);
        }
        return compiledArgs;
    }

    private int calculateArgsSizeWithCollectionArgs() {
        int startSize = argsOrReferences.size();
        for (Collection<?> collection : collectionArgs) {
            startSize += (collection.size() - 1);
        }
        return startSize;
    }

    private int calculateArgsSizeWithoutCollectionArgs() {
        return argsOrReferences.size() - collectionArgs.size();
    }

    private void populateCompiledArgs(boolean largeArgMode) {
        boolean foundReferenceArgument = false;
        int i = 0;
        for (Object arg : argsOrReferences) {
            if (arg instanceof AtomicReference<?>) {
                foundReferenceArgument = true;
                compiledArgs[i] = ((AtomicReference<?>) arg).get();
                i++;
            } else if (arg instanceof Collection<?>) {
                foundReferenceArgument = true;
                if (!largeArgMode) {
                    Collection<?> values = (Collection<?>) arg;
                    for (Object obj : values) {
                        compiledArgs[i] = obj;
                        i++;
                    }
                }
            } else {
                if (arg instanceof AtomicBoolean) { // Not a subclass of number so needs special handling
                    foundReferenceArgument = true;
                    compiledArgs[i] = ((AtomicBoolean) arg).get() ? 1 : 0;
                } else {
                    compiledArgs[i] = arg;
                }
                i++;
            }
        }
        hasImmutableArgs = !foundReferenceArgument;
    }

    @SuppressWarnings("serial")
    static class SimpleLruCache<K, V> extends LinkedHashMap<K, V> {

        private final int maxCapacity;

        public SimpleLruCache(int maxCapacity) {
            super(0, 0.75f, true);
            this.maxCapacity = maxCapacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxCapacity;
        }
    }
}
