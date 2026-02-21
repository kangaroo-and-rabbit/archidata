package org.atriasoft.archidata.dataAccess.model;

/**
 * Categorizes the role of a field in database operations.
 * Pre-computed at ClassModel construction time to avoid annotation lookups at runtime.
 */
public enum DbFieldAction {
	/** Standard field with no special behavior. */
	NORMAL,
	/** Primary key field (@Id). */
	PRIMARY_KEY,
	/** Auto-set on creation (@CreationTimestamp). */
	CREATION_TIMESTAMP,
	/** Auto-set on update (@UpdateTimestamp). */
	UPDATE_TIMESTAMP,
	/** Soft-delete marker (@DataDeleted). */
	DELETED,
	/** Not read by default (@DataNotRead). */
	NOT_READ,
	/** Serialized as JSON (@DataJson). */
	JSON,
	/** Managed by a DataAccessAddOn (relationships). */
	ADDON
}
