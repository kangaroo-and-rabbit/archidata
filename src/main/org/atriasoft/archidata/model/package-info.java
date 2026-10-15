/**
 * Base entity classes for database-backed models.
 *
 * <p>Provides base classes with common fields (id, timestamps, soft-delete)
 * that application models extend. Supports ObjectId, Long, and UUID primary keys.</p>
 *
 * <p>Pick the base according to the key and the delete policy:</p>
 * <ul>
 * <li>{@link org.atriasoft.archidata.model.GenericTiming}: only {@code createdAt} / {@code updatedAt}.</li>
 * <li>{@link org.atriasoft.archidata.model.OIDGenericData} (ObjectId),
 * {@link org.atriasoft.archidata.model.UUIDGenericData} (UUID) and
 * {@link org.atriasoft.archidata.model.GenericData} (Long): timestamps plus a primary key.</li>
 * <li>The {@code ...SoftDelete} variants, such as
 * {@link org.atriasoft.archidata.model.OIDGenericDataSoftDelete}, add a {@code deleted} flag;
 * {@link org.atriasoft.archidata.model.OIDGenericDataSoftAsyncHardDelete} lets the row be purged later.</li>
 * <li>{@link org.atriasoft.archidata.model.UserByToken} and
 * {@link org.atriasoft.archidata.model.Token}: identity models used by the authentication.</li>
 * </ul>
 *
 * <p>Example:</p>
 * {@snippet lang = java :
 * @Collection("task")
 * public class Task extends OIDGenericDataSoftDelete {
 * 	@NotNull
 * 	@Size(max = 200)
 * 	public String title;
 * 	public Boolean completed = false;
 * }
 * }
 *
 * <p>See <code>doc/data_model.md</code> in the repository
 * (<a href="https://github.com/kangaroo-and-rabbit/archidata/blob/develop/doc/data_model.md">online</a>).</p>
 *
 * @see org.atriasoft.archidata.model.token
 */
package org.atriasoft.archidata.model;
