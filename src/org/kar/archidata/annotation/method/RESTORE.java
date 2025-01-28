package org.kar.archidata.annotation.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.HttpMethod;

/**
* Indicates that the annotated method responds to HTTP RESTORE requests.
*
* @author Edouard DUPIN
* @see HttpMethod
*/
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("RESTORE")
public @interface RESTORE {}
