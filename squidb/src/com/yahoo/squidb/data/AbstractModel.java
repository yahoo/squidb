/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Field;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.PropertyVisitor;
import com.yahoo.squidb.sql.Property.PropertyWritingVisitor;
import com.yahoo.squidb.utility.Logger;
import com.yahoo.squidb.utility.SquidUtilities;

import java.util.HashMap;
import java.util.Set;

/**
 * Base class for models backed by a SQLite table or view. Attributes of a model are accessed and manipulated using
 * {@link Property} objects along with the {@link #get(Property) get} and {@link #set(Property, Object) set} methods.
 * <p>
 * Generated models automatically contain Property objects that correspond to the underlying columns in the table or
 * view as specified in their model spec definitions, and will have generated getter and setter methods for each
 * Property.
 * <p>
 * <h3>Data Source Ordering</h3>
 * When calling get(Property) or one of the generated getters, the model prioritizes values in the following order:
 * <ol>
 * <li>values explicitly set using set(Property, Object) or a generated setter, found in the set returned by
 * {@link #getSetValues()}</li>
 * <li>values written to the model as a result of fetching it using a {@link SquidDatabase} or constructing it from a
 * {@link SquidCursor}, found in the set returned by {@link #getDatabaseValues()}</li>
 * <li>default values, found in the set returned by {@link #getDefaultValues()}</li>
 * </ol>
 * If a value is not found in any of these places, an exception is thrown.
 * <p>
 * Transitory values (set using {@link #putTransitory(String, Object) putTransitory}) allow you to attach arbitrary
 * data that will not be saved to the database if you persist the model. Transitory values are not considered when
 * calling get(Property) or using generated getters; use {@link #getTransitory(String) getTransitory} to read these
 * values. Alternatively, use {@link #hasTransitory(String) checkTransitory} to merely check the presence of a
 * transitory value.
 * <p>
 * <h3>Interacting with Models</h3>
 * Models are usually created by fetching from a database or reading from a {@link SquidCursor} after querying a
 * database.
 *
 * <pre>
 * MyDatabase db = ...
 * Model model = db.fetch(Model.class, id, Model.PROPERTIES);
 * // or
 * SquidCursor&lt;Model&gt; cursor = db.query(Model.class, query);
 * cursor.moveToFirst();
 * Model model = new Model(cursor);
 * </pre>
 *
 * Models can also be instantiated in advance and populated with data from the current row of a SquidCursor.
 *
 * <pre>
 * model = new Model();
 * model.readPropertiesFromCursor(cursor);
 * </pre>
 *
 * @see com.yahoo.squidb.data.TableModel
 * @see com.yahoo.squidb.data.ViewModel
 */
public abstract class AbstractModel implements Cloneable {

    // --- static variables

    private static final ContentValuesSavingVisitor saver = new ContentValuesSavingVisitor();

    private static final ValueCastingVisitor valueCastingVisitor = new ValueCastingVisitor();

    // --- abstract methods

    /** Get the default values for this object */
    public abstract ValuesStorage getDefaultValues();

    // --- data store variables and management

    /** User set values */
    protected ValuesStorage setValues = null;

    /** Values from database */
    protected ValuesStorage values = null;

    /** Transitory Metadata (not saved in database) */
    protected HashMap<String, Object> transitoryData = null;

    /** Get the database-read values for this object */
    public ValuesStorage getDatabaseValues() {
        return values;
    }

    /** Get the user-set values for this object */
    public ValuesStorage getSetValues() {
        return setValues;
    }

    /** Get a list of all field/value pairs merged across data sources */
    public ValuesStorage getMergedValues() {
        ValuesStorage mergedValues = newValuesStorage();

        ValuesStorage defaultValues = getDefaultValues();
        if (defaultValues != null) {
            mergedValues.putAll(defaultValues);
        }

        if (values != null) {
            mergedValues.putAll(values);
        }

        if (setValues != null) {
            mergedValues.putAll(setValues);
        }

        return mergedValues;
    }

    protected abstract ValuesStorage newValuesStorage();

    /**
     * Clear all data on this model
     */
    public void clear() {
        values = null;
        setValues = null;
    }

    /**
     * Transfers all set values into values. This usually occurs when a model is saved in the database so that future
     * saves will not need to write all the data again. Users should not usually need to call this method.
     */
    public void markSaved() {
        if (values == null) {
            values = setValues;
        } else if (setValues != null) {
            values.putAll(setValues);
        }
        setValues = null;
    }

    /**
     * Use merged values to compare two models to each other. Must be of exactly the same class.
     */
    @Override
    public boolean equals(Object other) {
        return other != null && getClass().equals(other.getClass()) && getMergedValues()
                .equals(((AbstractModel) other).getMergedValues());
    }

    @Override
    public int hashCode() {
        return getMergedValues().hashCode() ^ getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "\n" +
                "set values:\n" + setValues + "\n" +
                "values:\n" + values + "\n";
    }

    @Override
    public AbstractModel clone() {
        AbstractModel clone;
        try {
            clone = (AbstractModel) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        if (setValues != null) {
            clone.setValues = newValuesStorage();
            clone.setValues.putAll(setValues);
        }

        if (values != null) {
            clone.values = newValuesStorage();
            clone.values.putAll(values);
        }
        return clone;
    }

    /**
     * Android-specific method for initializing object state from a Parcel object. The default implementation of this
     * method logs an error, as only its overridden version in AndroidTableModel and AndroidViewModel should ever be
     * called.
     *
     * @param source a Parcel object to read from
     */
    public void readFromParcel(Object source) {
        Logger.w("Called readFromParcel on a non-parcelable model", new Throwable());
    }

    /**
     * @return true if this model has values that have been changed
     */
    public boolean isModified() {
        return setValues != null && setValues.size() > 0;
    }

    // --- data retrieval

    /**
     * Copies values from the given {@link ValuesStorage} into the model. The values will be added to the model as read
     * values (i.e. will not be considered set values or mark the model as dirty).
     */
    public void readPropertiesFromValuesStorage(ValuesStorage values, Property<?>... properties) {
        prepareToReadProperties();

        if (values != null) {
            for (Property<?> property : properties) {
                if (values.containsKey(property.getName())) {
                    SquidUtilities.putInto(this.values, property.getName(),
                            getFromValues(property, values), true);
                }
            }
        }
    }

    /**
     * Reads all properties from the supplied cursor into the model. This will clear any user-set values.
     */
    public void readPropertiesFromCursor(SquidCursor<?> cursor) {
        prepareToReadProperties();

        for (Field<?> field : cursor.getFields()) {
            readFieldIntoModel(cursor, field);
        }
    }

    /**
     * Reads the specified properties from the supplied cursor into the model. This will clear any user-set values.
     */
    public void readPropertiesFromCursor(SquidCursor<?> cursor, Property<?>... properties) {
        prepareToReadProperties();

        for (Property<?> field : properties) {
            readFieldIntoModel(cursor, field);
        }
    }

    private void prepareToReadProperties() {
        if (values == null) {
            values = newValuesStorage();
        }

        // clears user-set values
        setValues = null;
        transitoryData = null;
    }

    private void readFieldIntoModel(SquidCursor<?> cursor, com.yahoo.squidb.sql.Field<?> field) {
        try {
            if (field instanceof Property<?>) {
                Property<?> property = (Property<?>) field;
                saver.save(property, values, cursor.get(property));
            }
        } catch (IllegalArgumentException e) {
            // underlying cursor may have changed, suppress
        }
    }

    /**
     * Return the value of the specified {@link Property}. The model prioritizes values as follows:
     * <ol>
     * <li>values explicitly set using {@link #set(Property, Object)} or a generated setter</li>
     * <li>values written to the model as a result of fetching it using a {@link SquidDatabase} or constructing it from
     * a {@link SquidCursor}</li>
     * <li>the set of default values as specified by {@link #getDefaultValues()}</li>
     * </ol>
     * If a value is not found in any of those places, an exception is thrown.
     *
     * @return the value of the specified property
     * @throws UnsupportedOperationException if the value is not found in the model
     */
    @SuppressWarnings("unchecked")
    public <TYPE> TYPE get(Property<TYPE> property) {
        if (setValues != null && setValues.containsKey(property.getName())) {
            return getFromValues(property, setValues);
        } else if (values != null && values.containsKey(property.getName())) {
            return getFromValues(property, values);
        } else if (getDefaultValues().containsKey(property.getName())) {
            return getFromValues(property, getDefaultValues());
        } else {
            throw new UnsupportedOperationException(property.getName()
                    + " not found in model. Make sure the value was set explicitly, read from a cursor,"
                    + " or that the model has a default value for this property.");
        }

    }

    @SuppressWarnings("unchecked")
    private <TYPE> TYPE getFromValues(Property<TYPE> property, ValuesStorage values) {
        Object value = values.get(property.getName());

        // Will throw a ClassCastException if the value could not be coerced to the correct type
        return (TYPE) property.accept(valueCastingVisitor, value);
    }

    /**
     * @param property the {@link Property} to check
     * @return true if a value for this property has been read from the database or set by the user
     */
    public boolean containsValue(Property<?> property) {
        return valuesContainsKey(setValues, property) || valuesContainsKey(values, property);
    }

    /**
     * @param property the {@link Property} to check
     * @return true if a value for this property has been read from the database or set by the user, and the value
     * stored is not null
     */
    public boolean containsNonNullValue(Property<?> property) {
        return (valuesContainsKey(setValues, property) && setValues.get(property.getName()) != null)
                || (valuesContainsKey(values, property) && values.get(property.getName()) != null);
    }

    /**
     * @param property the {@link Property} to check
     * @return true if this property has a value that was set by the user
     */
    public boolean fieldIsDirty(Property<?> property) {
        return valuesContainsKey(setValues, property);
    }

    private boolean valuesContainsKey(ValuesStorage values, Property<?> property) {
        return values != null && values.containsKey(property.getName());
    }

    // --- data storage

    /**
     * Check whether the user has changed this property value and it should be stored for saving in the database
     */
    protected <TYPE> boolean shouldSaveValue(Property<TYPE> property, TYPE newValue) {
        return shouldSaveValue(property.getName(), newValue);
    }

    protected boolean shouldSaveValue(String name, Object newValue) {
        // we've already decided to save it, so overwrite old value
        if (setValues.containsKey(name)) {
            return true;
        }

        // values contains this key, we should check it out
        if (values != null && values.containsKey(name)) {
            Object value = values.get(name);
            if (value == null) {
                if (newValue == null) {
                    return false;
                }
            } else if (value.equals(newValue)) {
                return false;
            }
        }

        // otherwise, good to save
        return true;
    }

    /**
     * Sets the specified {@link Property} to the given value. For generated models, it is preferred to call a
     * generated set[Property] method instead.
     *
     * @param property the property to set
     * @param value the new value for the property
     */
    public <TYPE> void set(Property<TYPE> property, TYPE value) {
        if (setValues == null) {
            setValues = newValuesStorage();
        }

        if (!shouldSaveValue(property, value)) {
            return;
        }

        saver.save(property, setValues, value);
    }

    /**
     * Analogous to {@link #readPropertiesFromValuesStorage(ValuesStorage, Property[])} but adds the values to the
     * model as set values, i.e. marks the model as dirty with these values.
     */
    public void setPropertiesFromValuesStorage(ValuesStorage values, Property<?>... properties) {
        if (values != null) {
            if (setValues == null) {
                setValues = newValuesStorage();
            }
            for (Property<?> property : properties) {
                String key = property.getName();
                if (values.containsKey(key)) {
                    Object value = property.accept(valueCastingVisitor, values.get(key));
                    if (shouldSaveValue(key, value)) {
                        SquidUtilities.putInto(this.setValues, property.getName(), value, true);
                    }
                }
            }
        }
    }

    /**
     * Clear the value for the given {@link Property}
     *
     * @param property the property to clear
     */
    public void clearValue(Property<?> property) {
        if (setValues != null && setValues.containsKey(property.getName())) {
            setValues.remove(property.getName());
        }

        if (values != null && values.containsKey(property.getName())) {
            values.remove(property.getName());
        }
    }

    // --- storing and retrieving transitory values

    /**
     * Add transitory data to the model. Transitory data is meant for developers to attach short-lived metadata to
     * models and is not persisted to the database.
     *
     * @param key the key for the transitory data
     * @param value the value for the transitory data
     * @see #getTransitory(String)
     */
    public void putTransitory(String key, Object value) {
        if (transitoryData == null) {
            transitoryData = new HashMap<String, Object>();
        }
        transitoryData.put(key, value);
    }

    /**
     * Get the transitory metadata object for the given key
     *
     * @param key the key for the transitory data
     * @return the transitory data if it exists, or null otherwise
     * @see #putTransitory(String, Object)
     */
    public Object getTransitory(String key) {
        if (transitoryData == null) {
            return null;
        }
        return transitoryData.get(key);
    }

    /**
     * Remove the transitory object for the specified key, if one exists
     *
     * @param key the key for the transitory data
     * @return the removed transitory value, or null if none existed
     * @see #putTransitory(String, Object)
     */
    public Object clearTransitory(String key) {
        if (transitoryData == null) {
            return null;
        }
        return transitoryData.remove(key);
    }

    /**
     * @return all transitory keys set on this model
     * @see #putTransitory(String, Object)
     */
    public Set<String> getAllTransitoryKeys() {
        if (transitoryData == null) {
            return null;
        }
        return transitoryData.keySet();
    }

    // --- convenience wrappers for using transitory data as flags

    /**
     * Convenience for using transitory data as a flag
     *
     * @param key the key for the transitory data
     * @return true if a transitory object is set for the given key, false otherwise
     */
    public boolean hasTransitory(String key) {
        return getTransitory(key) != null;
    }

    /**
     * Convenience for using transitory data as a flag. Removes the transitory data for this key if one existed.
     *
     * @param key the key for the transitory data
     * @return true if a transitory object is set for the given flag, false otherwise
     */
    public boolean checkAndClearTransitory(String key) {
        return clearTransitory(key) != null;
    }

    /**
     * Visitor that saves a value into a content values store
     */
    private static class ContentValuesSavingVisitor implements PropertyWritingVisitor<Void, ValuesStorage, Object> {

        public void save(Property<?> property, ValuesStorage newStore, Object value) {
            if (value != null) {
                property.accept(this, newStore, value);
            } else {
                newStore.putNull(property.getName());
            }
        }

        @Override
        public Void visitDouble(Property<Double> property, ValuesStorage dst, Object value) {
            dst.put(property.getName(), (Double) value);
            return null;
        }

        @Override
        public Void visitInteger(Property<Integer> property, ValuesStorage dst, Object value) {
            dst.put(property.getName(), (Integer) value);
            return null;
        }

        @Override
        public Void visitLong(Property<Long> property, ValuesStorage dst, Object value) {
            dst.put(property.getName(), (Long) value);
            return null;
        }

        @Override
        public Void visitString(Property<String> property, ValuesStorage dst, Object value) {
            dst.put(property.getName(), (String) value);
            return null;
        }

        @Override
        public Void visitBoolean(Property<Boolean> property, ValuesStorage dst, Object value) {
            if (value instanceof Boolean) {
                dst.put(property.getName(), (Boolean) value);
            } else if (value instanceof Integer) {
                dst.put(property.getName(), ((Integer) value) != 0);
            }
            return null;
        }

        @Override
        public Void visitBlob(Property<byte[]> property, ValuesStorage dst, Object value) {
            dst.put(property.getName(), (byte[]) value);
            return null;
        }

    }

    private static class ValueCastingVisitor implements PropertyVisitor<Object, Object> {

        @Override
        public Object visitInteger(Property<Integer> property, Object data) {
            if (data == null || data instanceof Integer) {
                return data;
            } else if (data instanceof Number) {
                return ((Number) data).intValue();
            } else if (data instanceof Boolean) {
                return (Boolean) data ? 1 : 0;
            } else if (data instanceof String) {
                try {
                    return Integer.valueOf((String) data);
                } catch (NumberFormatException e) {
                    // Suppress and throw the class cast
                }
            }
            throw new ClassCastException("Value " + data + " could not be cast to Integer");
        }

        @Override
        public Object visitLong(Property<Long> property, Object data) {
            if (data == null || data instanceof Long) {
                return data;
            } else if (data instanceof Number) {
                return ((Number) data).longValue();
            } else if (data instanceof Boolean) {
                return (Boolean) data ? 1L : 0L;
            } else if (data instanceof String) {
                try {
                    return Long.valueOf((String) data);
                } catch (NumberFormatException e) {
                    // Suppress and throw the class cast
                }
            }
            throw new ClassCastException("Value " + data + " could not be cast to Long");
        }

        @Override
        public Object visitDouble(Property<Double> property, Object data) {
            if (data == null || data instanceof Double) {
                return data;
            } else if (data instanceof Number) {
                return ((Number) data).doubleValue();
            } else if (data instanceof String) {
                try {
                    return Double.valueOf((String) data);
                } catch (NumberFormatException e) {
                    // Suppress and throw the class cast
                }
            }
            throw new ClassCastException("Value " + data + " could not be cast to Double");
        }

        @Override
        public Object visitString(Property<String> property, Object data) {
            if (data == null || data instanceof String) {
                return data;
            } else {
                return String.valueOf(data);
            }
        }

        @Override
        public Object visitBoolean(Property<Boolean> property, Object data) {
            if (data == null || data instanceof Boolean) {
                return data;
            } else if (data instanceof Number) {
                return ((Number) data).intValue() != 0;
            }
            throw new ClassCastException("Value " + data + " could not be cast to Boolean");
        }

        @Override
        public Object visitBlob(Property<byte[]> property, Object data) {
            if (data != null && !(data instanceof byte[])) {
                throw new ClassCastException("Data " + data + " could not be cast to byte[]");
            }
            return data;
        }

    }
}
