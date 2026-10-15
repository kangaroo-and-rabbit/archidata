package test.atriasoft.archidata.tools;

import org.atriasoft.archidata.tools.JWTWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestPublicKeyDeprecation {

	@Test
	@SuppressWarnings("removal")
	void topLevelPublicKeyIsDeprecatedForRemoval() {
		final Deprecated deprecated = org.atriasoft.archidata.tools.PublicKey.class.getAnnotation(Deprecated.class);
		Assertions.assertNotNull(deprecated);
		Assertions.assertTrue(deprecated.forRemoval());
	}

	@Test
	void jwtWrapperPublicKeyIsNotDeprecated() {
		Assertions.assertNull(JWTWrapper.PublicKey.class.getAnnotation(Deprecated.class));
	}
}
