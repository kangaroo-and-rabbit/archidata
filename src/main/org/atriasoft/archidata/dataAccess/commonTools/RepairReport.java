package org.atriasoft.archidata.dataAccess.commonTools;

import java.util.ArrayList;
import java.util.List;

/**
 * Report returned by {@link LinkRepairTools#repairLinks} summarizing what was found and fixed.
 */
public class RepairReport {
	private int documentsScanned;
	private int linksChecked;
	private int brokenLinksRemoved;
	private int missingLinksAdded;
	private int inconsistentLinksFixed;
	private final List<String> details = new ArrayList<>();

	public int getDocumentsScanned() {
		return this.documentsScanned;
	}

	public int getLinksChecked() {
		return this.linksChecked;
	}

	public int getBrokenLinksRemoved() {
		return this.brokenLinksRemoved;
	}

	public int getMissingLinksAdded() {
		return this.missingLinksAdded;
	}

	public int getInconsistentLinksFixed() {
		return this.inconsistentLinksFixed;
	}

	public List<String> getDetails() {
		return this.details;
	}

	public int getTotalFixes() {
		return this.brokenLinksRemoved + this.missingLinksAdded + this.inconsistentLinksFixed;
	}

	public void incrementDocumentsScanned() {
		this.documentsScanned++;
	}

	public void incrementLinksChecked() {
		this.linksChecked++;
	}

	public void incrementBrokenLinksRemoved() {
		this.brokenLinksRemoved++;
	}

	public void incrementMissingLinksAdded() {
		this.missingLinksAdded++;
	}

	public void incrementInconsistentLinksFixed() {
		this.inconsistentLinksFixed++;
	}

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
