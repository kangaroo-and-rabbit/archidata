package org.atriasoft.archidata.dataStreamEvent;

/**
 * Functional interface for listening to database change events.
 * Implementations receive notifications when MongoDB collections are modified.
 */
@FunctionalInterface
public interface ChangeNotificationListener {
	/**
	 * Called when a change is detected via MongoDB Change Stream
	 *
	 * @param event The change event containing operation details
	 */
	void onNotification(ChangeEvent event);
}
