/**
 * Declarative management of the MongoDB indexes.
 *
 * <p>Indexes are declared next to the entities — with
 * {@link org.atriasoft.archidata.annotation.Index} on the class,
 * {@link org.atriasoft.archidata.annotation.Indexed} on a property, or programmatically through
 * {@link org.atriasoft.archidata.index.IndexRegistry} — and resolved into
 * {@link org.atriasoft.archidata.index.IndexSpec} instances that the synchronization compares with
 * what the database actually holds.</p>
 */
package org.atriasoft.archidata.index;
