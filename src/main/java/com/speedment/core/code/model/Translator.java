/**
 *
 * Copyright (c) 2006-2015, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.core.code.model;

import com.speedment.core.config.model.Column;
import com.speedment.core.config.model.Dbms;
import com.speedment.core.config.model.ForeignKey;
import com.speedment.core.config.model.Index;
import com.speedment.core.config.model.PrimaryKeyColumn;
import com.speedment.core.config.model.Project;
import com.speedment.core.config.model.Schema;
import com.speedment.core.config.model.Table;
import com.speedment.core.config.model.aspects.Node;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A component that can translate a {@link Node} into something else. This 
 * interface is implemented to generate more files from the same database 
 * structure.
 * 
 * @author pemi
 * @param <T> The ConfigEntity type to use
 * @param <R> The type to translate into.
 * 
 * @See Node
 */
public interface Translator<T extends Node, R> extends Supplier<R> {

    /**
     * The node being translated.
     * 
     * @return the node.
     */
    T getNode();

    /**
     * Return this node or any ancestral node that is a {@link Project}. If no 
     * such node exists, an <code>IllegalStateException</code> is thrown.
     * 
     * @return the project node.
     */
    default Project project() {
        return getGenericConfigEntity(Project.class);
    }

    /**
     * Return this node or any ancestral node that is a {@link Dbms}. If no 
     * such node exists, an <code>IllegalStateException</code> is thrown.
     * 
     * @return the dbms node.
     */
    default Dbms dbms() {
        return getGenericConfigEntity(Dbms.class);
    }

    /**
     * Return this node or any ancestral node that is a {@link Schema}. If no 
     * such node exists, an <code>IllegalStateException</code> is thrown.
     * 
     * @return the schema node.
     */
    default Schema schema() {
        return getGenericConfigEntity(Schema.class);
    }

    /**
     * Return this node or any ancestral node that is a {@link Table}. If no 
     * such node exists, an <code>IllegalStateException</code> is thrown.
     * 
     * @return the table node.
     */
    default Table table() {
        return getGenericConfigEntity(Table.class);
    }

    /**
     * Return this node or any ancestral node that is a {@link Column}. If no 
     * such node exists, an <code>IllegalStateException</code> is thrown.
     * 
     * @return the column node.
     */
    default Column column() {
        return getGenericConfigEntity(Column.class);
    }

    /**
     * Returns a stream over all enabled columns in the node tree. Disabled
     * nodes will be ignored.
     * 
     * @return the enabled columns.
     * @see Column
     */
    default Stream<Column> columns() {
        return table().streamOf(Column.class).filter(Column::isEnabled);
    }

    /**
     * Returns a stream over all enabled indexes in the node tree. Disabled
     * nodes will be ignored.
     * 
     * @return the enabled indexes.
     * @see Index
     */
    default Stream<Index> indexes() {
        return table().streamOf(Index.class).filter(Index::isEnabled);
    }

    /**
     * Returns a stream over all enabled foreign keys in the node tree. Disabled
     * nodes will be ignored.
     * 
     * @return the enabled foreign keys.
     * @see ForeignKey
     */
    default Stream<ForeignKey> foreignKeys() {
        return table().streamOf(ForeignKey.class).filter(ForeignKey::isEnabled);
    }

    /**
     * Returns a stream over all enabled primary key columns in the node tree. 
     * Disabled nodes will be ignored.
     * 
     * @return the enabled primary key columns.
     * @see PrimaryKeyColumn
     */
    default Stream<PrimaryKeyColumn> primaryKeyColumns() {
        return table().streamOf(PrimaryKeyColumn.class).filter(PrimaryKeyColumn::isEnabled);
    }

    /**
     * Returns this node or one of the ancestor nodes if it matches the 
     * specified <code>Class</code>. If no such node exists, an 
     * <code>IllegalStateException</code> is thrown.
     * 
     * @return the node found.
     */
    default <E extends Node> E getGenericConfigEntity(Class<E> clazz) {
        if (clazz.isAssignableFrom(getNode().getInterfaceMainClass())) {
            @SuppressWarnings("unchecked")
            final E result = (E) getNode();
            return result;
        }

        return getNode().ancestor(clazz)
            .orElseThrow(() -> new IllegalStateException(
                getNode() + " is not a " + clazz.getSimpleName()
                + " and does not have a parent that is a " + clazz.getSimpleName()
            ));
    }
}