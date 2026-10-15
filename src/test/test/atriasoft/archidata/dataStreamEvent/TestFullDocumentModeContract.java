package test.atriasoft.archidata.dataStreamEvent;

import java.lang.reflect.Method;
import java.util.Set;

import org.atriasoft.archidata.dataStreamEvent.ChangeNotificationListener;
import org.atriasoft.archidata.dataStreamEvent.ChangeNotificationManager;
import org.atriasoft.archidata.dataStreamEvent.CollectionWatchBuilder;
import org.atriasoft.archidata.dataStreamEvent.ListenerRegistrationBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.mongodb.client.model.changestream.FullDocument;

/**
 * The FullDocument mode is global (chosen by {@code start()}): every API that takes a per-listener
 * or per-collection mode always ignores it and must say so by being deprecated (and by a warning at each
 * call, see {@link TestFullDocumentModeWarning}).
 */
class TestFullDocumentModeContract {

	private static void assertDeprecated(final Class<?> clazz, final String name, final Class<?>... parameterTypes)
			throws NoSuchMethodException {
		final Method method = clazz.getMethod(name, parameterTypes);
		Assertions.assertTrue(method.isAnnotationPresent(Deprecated.class),
				clazz.getSimpleName() + "." + name + " ignores its FullDocument mode and must be @Deprecated");
	}

	@Test
	void testPerListenerModeApisAreDeprecated() throws Exception {
		assertDeprecated(ChangeNotificationManager.class, "registerListener", ChangeNotificationListener.class,
				FullDocument.class);
		assertDeprecated(ChangeNotificationManager.class, "registerListener", ChangeNotificationListener.class,
				String.class, FullDocument.class);
		assertDeprecated(ChangeNotificationManager.class, "registerListener", ChangeNotificationListener.class,
				Set.class, FullDocument.class);
		assertDeprecated(ListenerRegistrationBuilder.class, "withMode", FullDocument.class);
		assertDeprecated(CollectionWatchBuilder.class, "fullDocument", FullDocument.class);
	}

	@Test
	void testModeLessApisAreNotDeprecated() throws Exception {
		Assertions.assertFalse(ChangeNotificationManager.class
				.getMethod("registerListener", ChangeNotificationListener.class, String.class)
				.isAnnotationPresent(Deprecated.class));
	}
}
