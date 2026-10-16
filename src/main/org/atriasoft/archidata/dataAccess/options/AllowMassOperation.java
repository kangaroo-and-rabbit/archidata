package org.atriasoft.archidata.dataAccess.options;

/**
 * Explicitly allows a write (update, delete, restore, {@code updateBsonDocuments}) to target every document
 * of a collection.
 *
 * <p>Without it, a write whose condition is missing or empty ({@code new Condition()},
 * {@code new Condition(Filters.empty())}, a filter built conditionally that stayed empty) is refused with a
 * {@code DataAccessException}: an application bug must not become a mass update or a mass deletion.
 *
 * <pre>{@code
 * DataAccess.updateBsonDocuments("user", new Document("$unset", new Document("legacy", "")),
 * 		new AllowMassOperation());
 * }</pre>
 */
public class AllowMassOperation extends QueryOption {
	/** Constructs the option allowing a write on the whole collection. */
	public AllowMassOperation() {
		// default constructor
	}
}
