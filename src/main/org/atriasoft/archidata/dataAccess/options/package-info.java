/**
 * Query options for data access operations.
 *
 * <p>Provides option classes such as {@link org.atriasoft.archidata.dataAccess.options.Condition},
 * {@link org.atriasoft.archidata.dataAccess.options.OrderBy},
 * {@link org.atriasoft.archidata.dataAccess.options.Limit} and
 * {@link org.atriasoft.archidata.dataAccess.options.FilterValue} that can be passed to data access
 * methods to control query behavior. All of them extend
 * {@link org.atriasoft.archidata.dataAccess.options.QueryOption}, and are passed as the trailing
 * varargs of {@link org.atriasoft.archidata.dataAccess.DataAccess} methods.</p>
 *
 * <ul>
 * <li>Selection: {@code Condition} (a MongoDB filter), {@code Limit}, {@code Skip}, {@code OrderBy}
 * (built from {@code OrderItem}s or {@code OrderBy.asc}/{@code OrderBy.desc}).</li>
 * <li>Columns: {@code FilterValue} (only these fields are written on update), {@code FilterOmit},
 * {@code ReadAllColumn}, {@code ForceReadOnlyField}.</li>
 * <li>Deletion: {@code AccessDeletedItems} (see soft-deleted rows), {@code ForceHardDelete}.</li>
 * <li>Keys and naming: {@code DirectPrimaryKey} (use the primary key given in the object),
 * {@code OverrideTableName}, {@code OptionRenameColumn}, {@code OptionSpecifyType}.</li>
 * </ul>
 *
 * <p>Example, updating a single field of the rows that match a condition:</p>
 * {@snippet lang = java :
 * final Article patch = new Article();
 * patch.status = "archived";
 * DataAccess.update(patch,
 * 		new Condition(Filters.eq("status", "draft")),
 * 		new FilterValue("status"));
 * }
 *
 * @see org.atriasoft.archidata.dataAccess
 */
package org.atriasoft.archidata.dataAccess.options;
