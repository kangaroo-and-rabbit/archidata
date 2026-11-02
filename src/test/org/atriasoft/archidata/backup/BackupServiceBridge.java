package org.atriasoft.archidata.backup;

/** Gives the tests, which live in another package, the constructor of {@link BackupService} that takes an engine. */
public final class BackupServiceBridge {
	private BackupServiceBridge() {
		// Utility class
	}

	/**
	 * Build a service on a test engine, which blocks or fails on demand.
	 * @param engine the engine
	 * @return the service
	 */
	public static BackupService on(final BackupEngine engine) {
		return new BackupService(engine);
	}
}
