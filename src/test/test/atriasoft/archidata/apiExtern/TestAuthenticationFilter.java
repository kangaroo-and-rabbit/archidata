package test.atriasoft.archidata.apiExtern;

import org.atriasoft.archidata.filter.AuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.Provider;

//@PreMatching
@Provider
@Priority(Priorities.AUTHENTICATION)
public class TestAuthenticationFilter extends AuthenticationFilter {
	/** Application name of the test server: the roles of the test tokens are granted for it. */
	public static final String APPLICATION_NAME = "karusic";
	final Logger logger = LoggerFactory.getLogger(TestAuthenticationFilter.class);

	public TestAuthenticationFilter() {
		super(APPLICATION_NAME);
	}

}
