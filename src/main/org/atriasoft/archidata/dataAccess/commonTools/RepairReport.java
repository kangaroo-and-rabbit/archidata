package org.atriasoft.archidata.dataAccess.commonTools;

import java.util.ArrayList;
import java.util.List;

/**
 * Report returned by {@link LinkRepairTools#repairLinks} summarizing what was found and fixed.
 */
public class RepairReport {
	/** Creates a new RepairReport with all counters initialized to zero. */
	public RepairReport() {}

	private int documentsScanned;
	private int linksChecked;
	private int brokenLinksRemoved;
	private int missingLinksAdded;
	private int inconsistentLinksFixed;
	private final List<String> details = new ArrayList<>();

	/**
	 * Returns the number of documents scanned during the repair.
	 *
	 * @return The count of documents scanned
	 */
	public int getDocumentsScanned() {
		return this.documentsScanned;
	}

	/**
	 * Returns the number of individual links checked during the repair.
	 *
	 * @return The count of links checked
	 */
	public int getLinksChecked() {
		return this.linksChecked;
	}

	/**
	 * Returns the number of broken links that were removed.
	 *
	 * @return The count of broken links removed
	 */
	public int getBrokenLinksRemoved() {
		return this.brokenLinksRemoved;
	}

	/**
	 * Returns the number of missing reverse links that were added.
	 *
	 * @return The count of missing links added
	 */
	public int getMissingLinksAdded() {
		return this.missingLinksAdded;
	}

	/**
	 * Returns the number of inconsistent links that were fixed.
	 *
	 * @return The count of inconsistent links fixed
	 */
	public int getInconsistentLinksFixed() {
		return this.inconsistentLinksFixed;
	}

	/**
	 * Returns the list of detailed messages describing each repair action taken.
	 *
	 * @return The list of detail messages
	 */
	public List<String> getDetails() {
		return this.details;
	}

	/**
	 * Returns the total number of fixes applied (broken removed + missing added + inconsistent fixed).
	 *
	 * @return The total count of all fixes
	 */
	public int getTotalFixes() {
		return this.brokenLinksRemoved + this.missingLinksAdded + this.inconsistentLinksFixed;
	}

	/** Increments the documents scanned counter by one. */
	public void incrementDocumentsScanned() {
		this.documentsScanned++;
	}

	/** Increments the links checked counter by one. */
	public void incrementLinksChecked() {
		this.linksChecked++;
	}

	/** Increments the broken links removed counter by one. */
	public void incrementBrokenLinksRemoved() {
		this.brokenLinksRemoved++;
	}

	/** Increments the missing links added counter by one. */
	public void incrementMissingLinksAdded() {
		this.missingLinksAdded++;
	}

	/** Increments the inconsistent links fixed counter by one. */
	public void incrementInconsistentLinksFixed() {
		this.inconsistentLinksFixed++;
	}

	/**
	 * Adds a detail message describing a specific repair action.
	 *
	 * @param detail The detail message to add
	 */
	public void addDetail(final String detail) {
		this.details.add(detail);
	}

	@Override
	public String toString() {
		return "RepairReport{scanned=" + this.documentsScanned + ", checked=" + this.linksChecked + ", brokenRemoved="
				+ this.brokenLinksRemoved + ", missingAdded=" + this.missingLinksAdded + ", inconsistentFixed="
				+ this.inconsistentLinksFixed + ", totalFixes=" + getTotalFixes() + "}";
	}
}
