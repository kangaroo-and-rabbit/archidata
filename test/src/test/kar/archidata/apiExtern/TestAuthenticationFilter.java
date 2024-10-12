package test.kar.archidata.apiExtern;

import org.kar.archidata.filter.AuthenticationFilter;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Priority;

//@PreMatching
@Provider
@Priority(Priorities.AUTHENTICATION)
public class TestAuthenticationFilter extends AuthenticationFilter {
	final Logger logger = LoggerFactory.getLogger(TestAuthenticationFilter.class);

	public TestAuthenticationFilter() {
		super("karusic");
	}

}
