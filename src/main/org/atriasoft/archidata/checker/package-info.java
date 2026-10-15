/**
 * Thread-local contexts used while a request is processed: database connection and validation.
 *
 * <ul>
 * <li>{@link org.atriasoft.archidata.checker.DataAccessConnectionContext}: shares one
 * {@link org.atriasoft.archidata.dataAccess.DBAccessMongo} connection per thread; the first context
 * opened on the thread owns and closes it.</li>
 * <li>{@link org.atriasoft.archidata.checker.ValidationContext}: binds a Jakarta Bean Validation
 * validator and a key-value store to the thread, so that the constraint validators (for example
 * {@link org.atriasoft.archidata.annotation.checker.UniqueInBaseId}) can read contextual
 * information such as the identifier of the entity being updated.</li>
 * </ul>
 *
 * <p>Both are {@link java.lang.AutoCloseable}:</p>
 * {@snippet lang = java :
 * try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
 * 	final DBAccessMongo da = ctx.get();
 * 	// ... database calls on this thread
 * }
 * }
 *
 * @see org.atriasoft.archidata.db
 * @see org.atriasoft.archidata.annotation.checker
 */
package org.atriasoft.archidata.checker;
