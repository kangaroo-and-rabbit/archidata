package org.atriasoft.archidata.backup;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

/**
 * What a client may know about one backup archive. An archive holds the whole database in clear: neither its
 * name nor its path nor its content leave the server.
 *
 * @param date date of the archive, in UTC, read from its sequence
 * @param size size of the archive in bytes
 * @param type kind of the archive
 * @param hasChecksum {@code true} when the {@code .sha256} file sits next to the archive
 */
public record BackupArchive(
		@NotNull Instant date,
		long size,
		@NotNull BackupArchiveType type,
		boolean hasChecksum) {}
