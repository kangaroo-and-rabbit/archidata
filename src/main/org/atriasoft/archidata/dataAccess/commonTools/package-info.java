/**
 * Utilities that keep the relation fields of the documents consistent.
 *
 * <p>Atomic add and remove of a value in the list of a document ({@code ListInDbTools}), maintenance of
 * the {@code @ManyToManyDoc} links ({@code ManyToManyTools}), reset of a field on every document
 * ({@code FieldTools}) and detection or repair of broken links ({@code LinkRepairTools},
 * {@code RepairReport}).</p>
 */
package org.atriasoft.archidata.dataAccess.commonTools;
