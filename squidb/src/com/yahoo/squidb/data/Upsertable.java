/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Criterion;
import com.yahoo.squidb.sql.TableStatement;

import javax.annotation.Nullable;

/**
 * Interface that can be applied to a generated {@link TableModel} class to make the model compatible with
 * {@link SquidDatabase#upsert(TableModel)}. Upsertable models are generally uniquely identified in their table by a
 * column or collection of columns referred to as a "logical key". Logical keys can be specified in a model spec
 * using the &#064;UpsertKey annotation.
 *
 * @see SquidDatabase#upsert(TableModel)
 * @see SquidDatabase#upsertWithOnConflict(TableModel, TableStatement.ConflictAlgorithm)  
 */
public interface Upsertable {

    /**
     * If an Upsertable model returns true for this method, and upsert() is called with a model that has a rowid set,
     * the logical upsert key will be ignored and upsert() will attempt only an update operation on the row with the
     * given id. By default, the upsert code generation will generate an implementation that returns true, and
     * upsert should generally be called with models that have no rowid set.
     *
     * @return true if the Upsertable model should use the rowid if one is set when calling upsert(), false if upsert
     * should always insert/update data (including the rowid) using the logical upsert key rather than the rowid.
     */
    boolean rowidSupersedesLogicalKey();

    /**
     * Construct a criterion on which to look up a (possibly) existing row using the logical upsert key columns. If
     * such a criterion cannot be constructed (e.g. if the model hasn't been fully populated such that necessary/valid
     * values are not available when it is called), implementations of this method may either return null to fail
     * quietly (in which case upsert() will return false), or throw a runtime exception to fail noisily.
     *
     * @return a criterion for looking up a (possibly) existing row using the logical upsert key columns
     */
    @Nullable
    Criterion getLogicalKeyLookupCriterion();

}
