package test.kar.archidata;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

public class StepwiseExtension implements ExecutionCondition, TestExecutionExceptionHandler {
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext extensionContext) {
		final ExtensionContext.Namespace namespace = namespaceFor(extensionContext);
		final ExtensionContext.Store store = storeFor(extensionContext, namespace);
		final String value = store.get(StepwiseExtension.class, String.class);
		return value == null ? ConditionEvaluationResult.enabled("No test failures in stepwise tests")
				: ConditionEvaluationResult
						.disabled(String.format("Stepwise test disabled due to previous failure in '%s'", value));
	}

	@Override
	public void handleTestExecutionException(final ExtensionContext extensionContext, final Throwable throwable)
			throws Throwable {
		final ExtensionContext.Namespace namespace = namespaceFor(extensionContext);
		final ExtensionContext.Store store = storeFor(extensionContext, namespace);
		store.put(StepwiseExtension.class, extensionContext.getDisplayName());
		throw throwable;
	}

	private ExtensionContext.Namespace namespaceFor(final ExtensionContext extensionContext) {
		return ExtensionContext.Namespace.create(StepwiseExtension.class, extensionContext.getParent());
	}

	private ExtensionContext.Store storeFor(
			final ExtensionContext extensionContext,
			final ExtensionContext.Namespace namespace) {
		return extensionContext.getParent().get().getStore(namespace);
	}
}