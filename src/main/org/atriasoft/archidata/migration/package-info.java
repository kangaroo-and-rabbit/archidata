/**
 * Database schema migration engine.
 *
 * <p>Provides a versioned migration system that tracks applied migrations
 * and executes pending ones in order. Supports both schema changes
 * and data transformations. The engine is forward-only.</p>
 *
 * <p>Entry points:</p>
 * <ul>
 * <li>{@link org.atriasoft.archidata.migration.MigrationEngine}: holds the ordered list of steps,
 * runs the pending ones ({@code migrateWaitAdmin} blocks on failure, {@code migrateErrorThrow}
 * throws a {@link org.atriasoft.archidata.migration.MigrationException}).</li>
 * <li>{@link org.atriasoft.archidata.migration.MigrationStep}: base class of a step; override
 * {@code getName()} and {@code generateStep()} and register the operations with
 * {@code addAction}.</li>
 * <li>{@link org.atriasoft.archidata.migration.MigrationInterface}: the contract implemented by a step.</li>
 * </ul>
 *
 * <p>Example:</p>
 * {@snippet lang = java :
 * public class Initialization extends MigrationStep {
 * 	@Override
 * 	public String getName() {
 * 		return "Initialization";
 * 	}
 *
 * 	@Override
 * 	public void generateStep() throws Exception {
 * 		addAction((final DBAccessMongo da) -> {
 * 			// create the initial data
 * 		});
 * 	}
 * }
 *
 * final MigrationEngine engine = new MigrationEngine();
 * engine.setInit(new Initialization());
 * engine.migrateWaitAdmin(dbConfig);
 * }
 *
 * <p>See <code>doc/migration.md</code> in the repository
 * (<a href="https://github.com/kangaroo-and-rabbit/archidata/blob/develop/doc/migration.md">online</a>).</p>
 *
 * @see org.atriasoft.archidata.migration.model
 */
package org.atriasoft.archidata.migration;
