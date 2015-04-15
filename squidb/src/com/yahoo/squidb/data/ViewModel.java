/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.data;

import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.PropertyWritingVisitor;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.SqlTable;
import com.yahoo.squidb.sql.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a row in a SQLite view. The properties of a ViewModel are defined in terms of the properties of other
 * models relevant to the query that backs the corresponding {@link View}.
 * <p>
 * To define a View for a ViewModel, include a static final {@link Query} object annotated with {@link
 * com.yahoo.squidb.annotations.ViewQuery} in the model spec definition. However, this is not required; you can use
 * ViewModels simply as models composed from other models and use them in conjunction with queries specific to those
 * compositions.
 */
public abstract class ViewModel extends AbstractModel {

    protected abstract TableMappingVisitors getTableMappingVisitors();

    /**
     * Extracts the properties in this ViewModel that originated from the specified model class and reads them into the
     * destination model object
     *
     * @param dst the destination model object
     * @return the destination model object
     */
    public <T extends AbstractModel> T mapToModel(T dst) {
        TableMappingVisitors visitors = getTableMappingVisitors();
        if (visitors != null) {
            @SuppressWarnings("unchecked")
            TableModelMappingVisitor<T> mapper = visitors.get((Class<T>) dst.getClass());
            if (mapper != null) {
                return mapper.map(this, dst);
            }
        }
        return dst;
    }

    public List<AbstractModel> mapToSourceModels() {
        List<AbstractModel> result = new ArrayList<AbstractModel>();
        TableMappingVisitors visitors = getTableMappingVisitors();
        if (visitors != null) {
            Set<Class<? extends AbstractModel>> sourceModels = visitors.allSourceModels();
            for (Class<? extends AbstractModel> cls : sourceModels) {
                try {
                    result.add(mapToModel(cls.newInstance()));
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }

    protected static class TableModelMappingVisitor<T extends AbstractModel> implements
            PropertyWritingVisitor<Void, T, ViewModel> {

        private final Property<?>[] relevantProperties;
        private final Map<Property<?>, Property<?>> aliasedPropertyMap;

        public TableModelMappingVisitor(Property<?>[] relevantProperties,
                Map<Property<?>, Property<?>> aliasedPropertyMap) {
            this.relevantProperties = relevantProperties;
            this.aliasedPropertyMap = aliasedPropertyMap;
        }

        public T map(ViewModel src, T dst) {
            for (Property<?> p : relevantProperties) {
                p.accept(this, dst, src);
            }
            return dst;
        }

        @Override
        public Void visitInteger(Property<Integer> property, T dst, ViewModel src) {
            Property<Integer> toSet = getPropertyToSet(property);
            if (src.containsValue(property)) {
                dst.set(toSet, src.get(property));
            }
            return null;
        }

        @Override
        public Void visitLong(Property<Long> property, T dst, ViewModel src) {
            Property<Long> toSet = getPropertyToSet(property);
            if (src.containsValue(property)) {
                dst.set(toSet, src.get(property));
            }
            return null;
        }

        @Override
        public Void visitDouble(Property<Double> property, T dst, ViewModel src) {
            Property<Double> toSet = getPropertyToSet(property);
            if (src.containsValue(property)) {
                dst.set(toSet, src.get(property));
            }
            return null;
        }

        @Override
        public Void visitString(Property<String> property, T dst, ViewModel src) {
            Property<String> toSet = getPropertyToSet(property);
            if (src.containsValue(property)) {
                dst.set(toSet, src.get(property));
            }
            return null;
        }

        @Override
        public Void visitBoolean(Property<Boolean> property, T dst, ViewModel src) {
            Property<Boolean> toSet = getPropertyToSet(property);
            if (src.containsValue(property)) {
                dst.set(toSet, src.get(property));
            }
            return null;
        }

        @Override
        public Void visitBlob(Property<byte[]> property, T dst, ViewModel src) {
            Property<byte[]> toSet = getPropertyToSet(property);
            if (src.containsValue(property)) {
                dst.set(toSet, src.get(property));
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private <PT> Property<PT> getPropertyToSet(Property<PT> property) {
            if (aliasedPropertyMap == null || !aliasedPropertyMap.containsKey(property)) {
                return property;
            }
            return (Property<PT>) aliasedPropertyMap.get(property);
        }

    }

    protected static void validateAliasedProperties(Property<?>[] aliasedPropertyArray) {
        Map<String, Integer> numOccurences = new HashMap<String, Integer>();
        Set<String> duplicates = new HashSet<String>();

        for (Property<?> p : aliasedPropertyArray) {
            String name = p.getName();
            if (numOccurences.containsKey(name)) {
                duplicates.add(name);
                numOccurences.put(name, numOccurences.get(name) + 1);
            } else {
                numOccurences.put(name, 1);
            }
        }

        for (int i = aliasedPropertyArray.length - 1; i >= 0; i--) {
            Property<?> base = aliasedPropertyArray[i];
            String name = base.getName();
            if (duplicates.contains(name)) {
                String alias;
                if (TableModel.ID_PROPERTY_NAME.equals(name) && base.table != null) {
                    alias = base.table.getName() + "Id";
                } else {
                    int occurence = numOccurences.get(name);
                    alias = name + "_" + occurence;
                }
                aliasedPropertyArray[i] = base.as(alias);
                numOccurences.put(name, numOccurences.get(name) - 1);
            }
        }
    }

    protected static class TableMappingVisitors {

        private Map<Class<? extends AbstractModel>, TableModelMappingVisitor<?>> map =
                new HashMap<Class<? extends AbstractModel>, TableModelMappingVisitor<?>>();

        private <T extends AbstractModel> void put(Class<T> cls, TableModelMappingVisitor<T> mapper) {
            map.put(cls, mapper);
        }

        @SuppressWarnings("unchecked")
        public <T extends AbstractModel> TableModelMappingVisitor<T> get(Class<T> cls) {
            return (TableModelMappingVisitor<T>) map.get(cls);
        }

        public Set<Class<? extends AbstractModel>> allSourceModels() {
            return map.keySet();
        }
    }

    protected static TableMappingVisitors generateTableMappingVisitors(Property<?>[] viewModelProperties,
            Property<?>[] aliasedProperties, Property<?>[] baseProperties) {

        TableMappingVisitors result = new TableMappingVisitors();

        Map<String, Integer> namesToPositions = new HashMap<String, Integer>();
        for (int i = 0; i < aliasedProperties.length; i++) {
            namesToPositions.put(aliasedProperties[i].getName(), i);
        }

        Map<SqlTable<?>, List<Property<?>>> tableToPropertyMap = new HashMap<SqlTable<?>, List<Property<?>>>();
        Map<SqlTable<?>, Map<Property<?>, Property<?>>> aliasedPropertiesMap =
                new HashMap<SqlTable<?>, Map<Property<?>, Property<?>>>();
        for (Property<?> p : viewModelProperties) {
            String name = p.getName();
            Integer position = namesToPositions.get(name);
            if (position == null) {
                continue;
            }

            Property<?> baseProperty = baseProperties[position];

            SqlTable<?> table = baseProperty.table;
            if (table == null) { // Not part of any other model, e.g. a function
                continue;
            }

            List<Property<?>> propertyList = tableToPropertyMap.get(table);
            if (propertyList == null) {
                propertyList = new ArrayList<Property<?>>();
                tableToPropertyMap.put(table, propertyList);
            }

            propertyList.add(p);

            if (!p.getName().equals(baseProperty.getName())) {
                Map<Property<?>, Property<?>> aliasedForTable = aliasedPropertiesMap.get(table);
                if (aliasedForTable == null) {
                    aliasedForTable = new HashMap<Property<?>, Property<?>>();
                    aliasedPropertiesMap.put(table, aliasedForTable);
                }
                aliasedForTable.put(p, baseProperty);
            }
        }

        for (Map.Entry<SqlTable<?>, List<Property<?>>> entry : tableToPropertyMap.entrySet()) {
            SqlTable<?> table = entry.getKey();
            List<Property<?>> properties = entry.getValue();
            Map<Property<?>, Property<?>> aliasMap = aliasedPropertiesMap.get(table);
            constructVisitor(table.getModelClass(), result, properties, aliasMap);
        }
        return result;
    }

    private static <T extends AbstractModel> void constructVisitor(Class<T> cls, TableMappingVisitors visitors,
            List<Property<?>> properties, Map<Property<?>, Property<?>> aliasMap) {
        if (cls != null) {
            visitors.put(cls, new TableModelMappingVisitor<T>(properties.toArray(new Property<?>[properties.size()]),
                    aliasMap));
        }
    }

}
