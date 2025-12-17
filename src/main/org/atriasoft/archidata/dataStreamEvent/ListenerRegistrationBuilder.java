package org.atriasoft.archidata.dataStreamEvent;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.bson.Document;

import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.OperationType;

/**
 * Builder for registering change notification listeners with client-side
 * filtering capabilities. This builder allows fluent configuration of filters,
 * modes, and other options before registering the listener.
 *
 * <p>
 * Client-side filters are applied AFTER MongoDB sends the events, allowing for
 * more complex filtering logic that may not be possible with server-side
 * aggregation pipelines. Multiple filters can be chained using AND logic.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * manager.createListenerBuilder(event -&gt; System.out.println(event), "users")
 * 		.filterField("role", "admin")
 * 		.filterOperation(OperationType.INSERT, OperationType.DELETE)
 * 		.register();
 * </pre>
 *
 * @see ChangeNotificationManager
 * @see ChangeNotificationListener
 */
public class ListenerRegistrationBuilder {
	private final ChangeNotificationManager manager;
	private final ChangeNotificationListener listener;
	private final Set<String> collectionNames; // null = global listener
	private FullDocument mode = null; // null = use manager default
	private Predicate<ChangeEvent> clientFilter = null;

	/**
	 * Internal constructor - use
	 * ChangeNotificationManager.createListenerBuilder() to create instances.
	 *
	 * @param manager The notification manager
	 * @param listener The listener to register
	 * @param collectionNames Collection names to listen to (null for global
	 *            listener)
	 */
	ListenerRegistrationBuilder(final ChangeNotificationManager manager, final ChangeNotificationListener listener,
			final Set<String> collectionNames) {
		this.manager = manager;
		this.listener = listener;
		this.collectionNames = collectionNames;
	}

	/**
	 * Specify the FullDocument mode for this listener
	 *
	 * @param mode The FullDocument mode (DEFAULT, UPDATE_LOOKUP, WHEN_AVAILABLE)
	 * @return this builder for chaining
	 */
	public ListenerRegistrationBuilder withMode(final FullDocument mode) {
		this.mode = mode;
		return this;
	}

	/**
	 * Add a simple field equality filter.
	 * The filter checks: fullDocument.get(fieldName).equals(expectedValue)
	 *
	 * @param fieldName The field name in the document
	 * @param expectedValue The expected value
	 * @return this builder for chaining
	 */
	public ListenerRegistrationBuilder filterField(final String fieldName, final Object expectedValue) {
		final Predicate<ChangeEvent> fieldFilter = event -> {
			if (!event.hasFullDocument()) {
				return false; // No document = filter fails
			}
			final Document doc = event.getFullDocument();
			final Object actualValue = doc.get(fieldName);
			return Objects.equals(actualValue, expectedValue);
		};

		// Combine with existing filter (AND logic)
		if (this.clientFilter == null) {
			this.clientFilter = fieldFilter;
		} else {
			this.clientFilter = this.clientFilter.and(fieldFilter);
		}
		return this;
	}

	/**
	 * Add a custom filter predicate.
	 * The predicate receives the ChangeEvent and returns true to keep, false to
	 * ignore.
	 *
	 * @param predicate Lambda filter (event -> boolean)
	 * @return this builder for chaining
	 */
	public ListenerRegistrationBuilder filter(final Predicate<ChangeEvent> predicate) {
		if (this.clientFilter == null) {
			this.clientFilter = predicate;
		} else {
			this.clientFilter = this.clientFilter.and(predicate);
		}
		return this;
	}

	/**
	 * Filter by operation type
	 *
	 * @param operations The operation types to accept (INSERT, UPDATE, etc.)
	 * @return this builder for chaining
	 */
	public ListenerRegistrationBuilder filterOperation(final OperationType... operations) {
		final Set<OperationType> allowedOps = Set.of(operations);
		final Predicate<ChangeEvent> opFilter = event -> allowedOps.contains(event.getOperationType());

		if (this.clientFilter == null) {
			this.clientFilter = opFilter;
		} else {
			this.clientFilter = this.clientFilter.and(opFilter);
		}
		return this;
	}

	/**
	 * Filter by collection name (useful for global listeners)
	 *
	 * @param collections Collection names to accept
	 * @return this builder for chaining
	 */
	public ListenerRegistrationBuilder filterCollection(final String... collections) {
		final Set<String> allowedCollections = Set.of(collections);
		final Predicate<ChangeEvent> collFilter = event -> allowedCollections.contains(event.getCollectionName());

		if (this.clientFilter == null) {
			this.clientFilter = collFilter;
		} else {
			this.clientFilter = this.clientFilter.and(collFilter);
		}
		return this;
	}

	/**
	 * Registers the listener with all configured filters and options. This is the
	 * final step in the builder chain.
	 *
	 * <p>
	 * After calling this method, the listener will start receiving change events
	 * that pass all configured filters. If the manager is already running, the
	 * worker for the collection will be started or updated if needed.
	 * </p>
	 */
	public void register() {
		this.manager.registerListenerInternal(this.listener, this.collectionNames, this.mode, this.clientFilter);
	}
}
