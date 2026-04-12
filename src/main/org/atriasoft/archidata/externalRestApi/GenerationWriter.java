package org.atriasoft.archidata.externalRestApi;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared utility for writing generated files to disk with smart diffing
 * and obsolete file detection. Used by both TypeScript and Python generators.
 */
public class GenerationWriter {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenerationWriter.class);

	/** Minimum depth required for output directory path (safety guard). */
	private static final int MINIMUM_PATH_DEPTH = 3;

	/** Forbidden root directories (Linux, macOS, Windows). */
	private static final List<String> FORBIDDEN_ROOTS = List.of(
			// Linux / macOS system directories
			"/", "/bin", "/boot", "/dev", "/etc", "/lib", "/lib64", "/opt",
			"/proc", "/root", "/run", "/sbin", "/sys", "/usr", "/var",
			// macOS specific
			"/System", "/Library", "/Applications",
			// Windows common roots (normalized with forward slashes)
			"C:/", "C:/Windows", "C:/Program Files", "C:/Program Files (x86)", "C:/ProgramData");

	/** Private constructor to prevent instantiation of this utility class. */
	private GenerationWriter() {
		// Utility class
	}

	/**
	 * Validates that the output directory is safe for code generation.
	 * Rejects system directories, user home roots, and paths that are too shallow.
	 *
	 * @param outputDir the output directory to validate
	 * @throws IOException if the path is considered unsafe
	 */
	private static void validateOutputDirectory(final Path outputDir) throws IOException {
		final Path absolutePath = outputDir.toAbsolutePath().normalize();
		final String pathStr = absolutePath.toString().replace('\\', '/');
		// Check against forbidden system roots
		for (final String forbidden : FORBIDDEN_ROOTS) {
			if (pathStr.equalsIgnoreCase(forbidden)) {
				throw new IOException("Refusing to generate into system directory: " + absolutePath);
			}
		}
		// Check that we are not directly in a user home directory
		// Linux/macOS: /home/<user> or /Users/<user>
		if (pathStr.matches("(?i)^/(home|Users)/[^/]+/?$")) {
			throw new IOException("Refusing to generate directly into user home directory: " + absolutePath);
		}
		// Windows: C:/Users/<user>
		if (pathStr.matches("(?i)^[A-Z]:/Users/[^/]+/?$")) {
			throw new IOException("Refusing to generate directly into user home directory: " + absolutePath);
		}
		// Minimum depth safety: path must have at least MINIMUM_PATH_DEPTH components
		final int nameCount = absolutePath.getNameCount();
		if (nameCount < MINIMUM_PATH_DEPTH) {
			throw new IOException(
					"Output directory path is too shallow (" + nameCount + " components, minimum "
							+ MINIMUM_PATH_DEPTH + "): " + absolutePath);
		}
	}

	/**
	 * Writes generated files to disk, skipping unchanged files and detecting obsolete ones.
	 *
	 * @param generation map of relative paths to generated content
	 * @param outputDir the root output directory
	 * @param fileExtension the file extension to filter for obsolete detection (e.g. ".ts", ".py")
	 * @param deleteObsoleteFiles if true, delete obsolete files; if false, log a warning
	 * @throws IOException if file operations fail
	 */
	public static void writeGeneratedFiles(
			final Map<Path, String> generation,
			final Path outputDir,
			final String fileExtension,
			final boolean deleteObsoleteFiles) throws IOException {
		validateOutputDirectory(outputDir);
		if (Files.notExists(outputDir)) {
			Files.createDirectories(outputDir);
		}
		final Set<Path> generatedPaths = new HashSet<>();
		int writtenCount = 0;
		int skippedCount = 0;
		for (final Map.Entry<Path, String> entry : generation.entrySet()) {
			final Path fullPath = outputDir.resolve(entry.getKey()).normalize();
			generatedPaths.add(fullPath);
			final Path pathParent = fullPath.getParent();
			if (Files.notExists(pathParent)) {
				Files.createDirectories(pathParent);
			}
			if (Files.exists(fullPath)) {
				final String existingContent = Files.readString(fullPath, StandardCharsets.UTF_8);
				if (existingContent.equals(entry.getValue())) {
					LOGGER.debug("Skipping unchanged file: {}", fullPath);
					skippedCount++;
					continue;
				}
			}
			try (final FileWriter writer = new FileWriter(fullPath.toFile(), StandardCharsets.UTF_8)) {
				writer.write(entry.getValue());
			}
			LOGGER.debug("Written file: {}", fullPath);
			writtenCount++;
		}
		LOGGER.info("Generation complete: {} files written, {} files unchanged", writtenCount, skippedCount);
		// Detect obsolete files
		final Set<Path> existingFiles = new HashSet<>();
		collectFilesWithExtension(outputDir, fileExtension, existingFiles);
		for (final Path existingFile : existingFiles) {
			if (!generatedPaths.contains(existingFile)) {
				if (deleteObsoleteFiles) {
					Files.delete(existingFile);
					LOGGER.info("Deleted obsolete file: {}", existingFile);
				} else {
					LOGGER.warn("Obsolete generated file should be deleted: {}", existingFile);
				}
			}
		}
	}

	/**
	 * Recursively collects all files with the given extension in a directory.
	 *
	 * @param directory the directory to scan
	 * @param fileExtension the file extension to match (e.g. ".ts")
	 * @param result the set to add matching file paths to
	 * @throws IOException if directory traversal fails
	 */
	private static void collectFilesWithExtension(
			final Path directory,
			final String fileExtension,
			final Set<Path> result) throws IOException {
		if (Files.notExists(directory)) {
			return;
		}
		try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
			for (final Path entry : stream) {
				if (Files.isDirectory(entry)) {
					collectFilesWithExtension(entry, fileExtension, result);
				} else if (entry.getFileName().toString().endsWith(fileExtension)) {
					result.add(entry.normalize());
				}
			}
		}
	}
}
