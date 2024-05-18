package org.kar.archidata.externalRestApi.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.kar.archidata.annotation.AsyncType;
import org.kar.archidata.annotation.TypeScriptProgress;
import org.kar.archidata.dataAccess.DataFactoryZod.ClassElement;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;

public class ApiTool {

	public static String apiAnnotationGetPath(final Class<?> element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Path.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((Path) annotation[0]).value();
	}

	public static List<String> apiAnnotationProduces(final Class<?> element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Produces.class);
		if (annotation.length == 0) {
			return null;
		}
		return Arrays.asList(((Produces) annotation[0]).value());
	}

	public static List<String> apiAnnotationProduces(final Method element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Produces.class);
		if (annotation.length == 0) {
			return null;
		}
		return Arrays.asList(((Produces) annotation[0]).value());
	}

	public static boolean apiAnnotationTypeScriptProgress(final Method element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(TypeScriptProgress.class);
		if (annotation.length == 0) {
			return false;
		}
		return true;
	}

	public static List<String> apiAnnotationProduces(final Class<?> clazz, final Method method) throws Exception {
		final List<String> data = apiAnnotationProduces(method);
		if (data != null) {
			return data;
		}
		return apiAnnotationProduces(clazz);
	}

	public static List<String> apiAnnotationProduces2(final List<String> parentProduce, final Method method)
			throws Exception {
		final List<String> data = apiAnnotationProduces(method);
		if (data != null) {
			return data;
		}
		return parentProduce;
	}

	public static String apiAnnotationGetOperationDescription(final Method element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Operation.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((Operation) annotation[0]).description();
	}

	public static String apiAnnotationGetPath(final Method element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Path.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((Path) annotation[0]).value();
	}

	public static String apiAnnotationGetTypeRequest(final Method element) throws Exception {
		if (element.getDeclaredAnnotationsByType(GET.class).length == 1) {
			return "GET";
		}
		if (element.getDeclaredAnnotationsByType(POST.class).length == 1) {
			return "POST";
		}
		if (element.getDeclaredAnnotationsByType(PUT.class).length == 1) {
			return "PUT";
		}
		if (element.getDeclaredAnnotationsByType(PATCH.class).length == 1) {
			return "PATCH";
		}
		if (element.getDeclaredAnnotationsByType(DELETE.class).length == 1) {
			return "DELETE";
		}
		return null;
	}

	public static RestTypeRequest apiAnnotationGetTypeRequest2(final Method element) throws Exception {
		if (element.getDeclaredAnnotationsByType(GET.class).length == 1) {
			return RestTypeRequest.GET;
		}
		if (element.getDeclaredAnnotationsByType(POST.class).length == 1) {
			return RestTypeRequest.POST;
		}
		if (element.getDeclaredAnnotationsByType(PUT.class).length == 1) {
			return RestTypeRequest.PUT;
		}
		if (element.getDeclaredAnnotationsByType(PATCH.class).length == 1) {
			return RestTypeRequest.PATCH;
		}
		if (element.getDeclaredAnnotationsByType(DELETE.class).length == 1) {
			return RestTypeRequest.DELETE;
		}
		return null;
	}

	public static String apiAnnotationGetPathParam(final Parameter element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(PathParam.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((PathParam) annotation[0]).value();
	}

	public static String apiAnnotationGetQueryParam(final Parameter element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(QueryParam.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((QueryParam) annotation[0]).value();
	}

	public static String apiAnnotationGetFormDataParam(final Parameter element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(FormDataParam.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((FormDataParam) annotation[0]).value();
	}

	public static Class<?>[] apiAnnotationGetAsyncType(final Parameter element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(AsyncType.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((AsyncType) annotation[0]).value();
	}

	public static Class<?>[] apiAnnotationGetAsyncType(final Method element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(AsyncType.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((AsyncType) annotation[0]).value();
	}

	public static List<String> apiAnnotationGetConsumes(final Method element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Consumes.class);
		if (annotation.length == 0) {
			return null;
		}
		return Arrays.asList(((Consumes) annotation[0]).value());
	}

	public static List<String> apiAnnotationGetConsumes(final Class<?> element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Consumes.class);
		if (annotation.length == 0) {
			return null;
		}
		return Arrays.asList(((Consumes) annotation[0]).value());
	}

	public static List<String> apiAnnotationGetConsumes(final Class<?> clazz, final Method method) throws Exception {
		final List<String> data = apiAnnotationGetConsumes(method);
		if (data != null) {
			return data;
		}
		return apiAnnotationGetConsumes(clazz);
	}

	public static List<String> apiAnnotationGetConsumes2(final List<String> parentConsume, final Method method)
			throws Exception {
		final List<String> data = apiAnnotationGetConsumes(method);
		if (data != null) {
			return data;
		}
		return parentConsume;
	}

	public static boolean apiAnnotationIsContext(final Parameter element) throws Exception {
		return element.getDeclaredAnnotationsByType(Context.class).length != 0;
	}

	public static String convertInTypeScriptType(final List<ClassElement> tmp, final boolean isList) {
		String out = "";
		for (final ClassElement elem : tmp) {
			if (out.length() != 0) {
				out += " | ";
			}
			out += elem.tsTypeName;
			if (isList) {
				out += "[]";
			}
		}
		return out;
	}

	public static String convertInTypeScriptCheckType(final List<ClassElement> tmp) {
		String out = "";
		for (final ClassElement elem : tmp) {
			if (out.length() != 0) {
				out += " | ";
			}
			out += elem.tsCheckType;
		}
		return out;
	}
}
