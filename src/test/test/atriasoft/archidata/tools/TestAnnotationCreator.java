package test.atriasoft.archidata.tools;

import org.atriasoft.archidata.annotation.apiGenerator.ApiGenerationMode;
import org.atriasoft.archidata.tools.AnnotationCreator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestAnnotationCreator {

	@Test
	void testCreateAnnotationWithValues() {
		final ApiGenerationMode annotation = AnnotationCreator.createAnnotation(ApiGenerationMode.class, "read", true,
				"create", false, "update", false);
		Assertions.assertTrue(annotation.read());
		Assertions.assertFalse(annotation.create());
		Assertions.assertFalse(annotation.update());
	}

	@Test
	void testCreateAnnotationWithDefaults() {
		final ApiGenerationMode annotation = AnnotationCreator.createAnnotation(ApiGenerationMode.class);
		// Default values from the annotation definition
		Assertions.assertNotNull(annotation);
	}
}
