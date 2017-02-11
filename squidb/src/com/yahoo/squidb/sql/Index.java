/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.utility.SquidUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * A SQLite Index
 */
public class Index {

    private final String name;
    private final Table table;
    private final boolean unique;
    private final List<Property<?>> properties;

    public Index(@Nonnull String name, @Nonnull Table table, boolean unique, @Nonnull Property<?>... properties) {
        this(name, table, unique, SquidUtilities.asList(properties));
    }

    public Index (@Nonnull String name, @Nonnull Table table, boolean unique,
            @Nonnull List<? extends Property<?>> properties) {
        this.name = name;
        this.table = table;
        this.unique = unique;
        this.properties = Collections.unmodifiableList(new ArrayList<>(properties));
    }

    /**
     * @return the name of this Index
     */
    @Nonnull
    public String getName() {
        return name;
    }

    /**
     * @return the {@link Table} on which this Index is created
     */
    @Nonnull
    public Table getTable() {
        return table;
    }

    /**
     * @return true if this is a unique Index, false otherwise. Unique indexes do not allow duplicate entries.
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * @return the {@link Property properties} representing columns indexed by this Index
     */
    @Nonnull
    public List<Property<?>> getProperties() {
        return properties;
    }

}
