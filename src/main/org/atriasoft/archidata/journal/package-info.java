/**
 * Periodic incremental journal of the modified documents of the database.
 *
 * <p>{@link org.atriasoft.archidata.journal.ChangeJournalEngine} captures, collection by
 * collection, the documents modified since the date stored in the per-collection marker
 * ({@link org.atriasoft.archidata.journal.ChangeJournalMarker}) and appends them to a single
 * journal collection ({@link org.atriasoft.archidata.journal.ChangeJournalEntry}) holding the
 * source collection name, the serialized document and the record date.</p>
 *
 * <p>{@link org.atriasoft.archidata.journal.ChangeJournalPurge} trims that journal in a separate
 * job, keeping the N most recent versions of every document plus everything younger than a
 * configured maximum age.</p>
 */
package org.atriasoft.archidata.journal;
