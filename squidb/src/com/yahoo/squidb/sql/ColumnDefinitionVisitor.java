/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of {@link com.yahoo.squidb.sql.Property.PropertyVisitor}that builds column definitions for
 * {@link Property}s
 */
public class ColumnDefinitionVisitor implements Property.PropertyVisitor<Void, StringBuilder> {
    @Nullable
    private Void appendColumnDefinition(@Nonnull String type, @Nonnull Property<?> property,
            @Nonnull StringBuilder sql) {
        sql.append(property.getName()).append(" ").append(type);
        if (!SqlUtils.isEmpty(property.getColumnDefinition())) {
            sql.append(" ").append(property.getColumnDefinition());
        }
        return null;
    }

    @Override
    @Nullable
    public Void visitDouble(@Nonnull Property<Double> property, @Nonnull StringBuilder sql) {
        return appendColumnDefinition("REAL", property, sql);
    }

    @Override
    @Nullable
    public Void visitInteger(@Nonnull Property<Integer> property, @Nonnull StringBuilder sql) {
        return appendColumnDefinition("INTEGER", property, sql);
    }

    @Override
    @Nullable
    public Void visitLong(@Nonnull Property<Long> property, @Nonnull StringBuilder sql) {
        return appendColumnDefinition("INTEGER", property, sql);
    }

    @Override
    @Nullable
    public Void visitString(@Nonnull Property<String> property, @Nonnull StringBuilder sql) {
        return appendColumnDefinition("TEXT", property, sql);
    }

    @Override
    @Nullable
    public Void visitBoolean(@Nonnull Property<Boolean> property, @Nonnull StringBuilder sql) {
        return appendColumnDefinition("INTEGER", property, sql);
    }

    @Override
    @Nullable
    public Void visitBlob(@Nonnull Property<byte[]> property, @Nonnull StringBuilder sql) {
        return appendColumnDefinition("BLOB", property, sql);
    }
}
