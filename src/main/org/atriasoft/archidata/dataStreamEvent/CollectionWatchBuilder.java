package org.atriasoft.archidata.dataStreamEvent;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.FullDocument;

/**
 * Builder for configuring MongoDB server-side filtering on Change Streams.
 * Allows fluent configuration of aggregation pipelines and FullDocument mode
 * before starting to watch a collection.
 *
 * <p>
 * Server-side filtering is more efficient than client-side filtering because
 * MongoDB filters the events before sending them over the network. This builder
 * constructs an aggregation pipeline that MongoDB executes on the change stream.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * manager.watch("users")
 * 		.fullDocument(FullDocument.UPDATE_LOOKUP)
 * 		.onlyUpdates()
 * 		.whenFieldUpdated("email")
 * 		.start();
 * </pre>
 *
 * <p>
 * Note: Server-side filters are applied by MongoDB and can significantly reduce
 * network traffic and processing overhead compared to client-side filters.
 * However, server-side filters are more limited in what they can express.
 * </p>
 *
 * @see ChangeNotificationManager
 * @see ListenerRegistrationBuilder
 */
public class CollectionWatchBuilder {
	private final ChangeNotificationManager manager;
	private final String collectionName;
	private FullDocument fullDocumentMode = FullDocument.DEFAULT;
	private final List<Bson> pipeline = new ArrayList<>();

	/**
	 * Internal constructor - use ChangeNotificationManager.watch() to create
	 * instances.
	 *
	 * @param manager The notification manager
	 * @param collectionName The name of the collection to watch
	 */
	CollectionWatchBuilder(final ChangeNotificationManager manager, final String collectionName) {
		this.manager = manager;
		this.collectionName = collectionName;
	}

	/**
	 * Set the FullDocument mode for this watch
	 *
	 * @param mode The FullDocument mode
	 * @return this builder for chaining
	 */
	public CollectionWatchBuilder fullDocument(final FullDocument mode) {
		this.fullDocumentMode = mode;
		return this;
	}

	/**
	 * Add a general filter to the aggregation pipeline
	 *
	 * @param filter MongoDB filter
	 * @return this builder for chaining
	 */
	public CollectionWatchBuilder filter(final Bson filter) {
		this.pipeline.add(Aggregates.match(filter));
		return this;
	}

	/**
	 * Filter to only receive INSERT operations
	 *
	 * @return this builder for chaining
	 */
	public CollectionWatchBuilder onlyInserts() {
		return filter(Filters.eq("operationType", "insert"));
	}

	/**
	 * Filter to only receive UPDATE operations
	 *
	 * @return this builder for chaining
	 */
	public CollectionWatchBuilder onlyUpdates() {
		return filter(Filters.eq("operationType", "update"));
	}

	/**
	 * Filter to only receive DELETE operations
	 *
	 * @return this builder for chaining
	 */
	public CollectionWatchBuilder onlyDeletes() {
		return filter(Filters.eq("operationType", "delete"));
	}

	/**
	 * Filter to specific operation types
	 *
	 * @param operations Operation type strings ("insert", "update", "delete", etc.)
	 * @return this builder for chaining
	 */
	public CollectionWatchBuilder onOperations(final String... operations) {
		return filter(Filters.in("operationType", operations));
	}

	/**
	 * Filter where a field equals a specific value
	 *
	 * @param field Field name (will be prefixed with "fullDocument.")
	 * @param value Expected value
	 * @return this builder for chaining
	 */
	public CollectionWatchBuilder whereField(final String field, final Object value) {
		return filter(Filters.eq("fullDocument." + field, value));
	}

	/**
	 * Filter where a field exists
	 *
	 * @param field Field name (will be prefixed with "fullDocument.")
	 * @return this builder for chaining
	 */
	public CollectionWatchBuilder whereFieldExists(final String field) {
		return filter(Filters.exists("fullDocument." + field));
	}

	/**
	 * Add a filter specifically for UPDATE operations
	 *
	 * @param updateFilter Filter to apply to updates
	 * @return this builder for chaining
	 */
	public CollectionWatchBuilder filterOnUpdate(final Bson updateFilter) {
		this.pipeline.add(Aggregates.match(Filters.eq("operationType", "update")));
		this.pipeline.add(Aggregates.match(updateFilter));
		return this;
	}

	/**
	 * Filter to only receive events when a specific field is updated
	 *
	 * @param field The field name to watch
	 * @return this builder for chaining
	 */
	public CollectionWatchBuilder whenFieldUpdated(final String field) {
		return filterOnUpdate(Filters.exists("updateDescription.updatedFields." + field));
	}

	/**
	 * Starts watching the collection with all configured server-side filters and
	 * options. This is the final step in the builder chain.
	 *
	 * <p>
	 * After calling this method, a ChangeStreamWorker will be created (if the
	 * manager is running) and will start monitoring the collection for changes
	 * that match the configured pipeline.
	 * </p>
	 *
	 * <p>
	 * Note: You still need to register listeners to actually receive the events.
	 * This method only configures the server-side filtering.
	 * </p>
	 */
	public void start() {
		this.manager.startWatchingCollection(this.collectionName, this.fullDocumentMode,
				this.pipeline.isEmpty() ? null : this.pipeline);
	}
}
