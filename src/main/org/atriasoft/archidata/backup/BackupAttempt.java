package org.atriasoft.archidata.backup;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

/**
 * Outcome of a backup attempt, a start refused for lack of space included. The reason of a failure stays in the log of
 * the server.
 *
 * @param date date at which the attempt started; a successful attempt has an archive of this exact date
 * @param success {@code true} when the archive was written
 */
public record BackupAttempt(
		@NotNull Instant date,
		boolean success) {}
