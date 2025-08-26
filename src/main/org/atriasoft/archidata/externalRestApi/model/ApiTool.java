package org.atriasoft.archidata.externalRestApi.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.apiGenerator.ApiAsyncType;
import org.atriasoft.archidata.annotation.apiGenerator.ApiInputOptional;
import org.atriasoft.archidata.annotation.apiGenerator.ApiTypeScriptProgress;
import org.atriasoft.archidata.annotation.method.ARCHIVE;
import org.atriasoft.archidata.annotation.method.CALL;
import org.atriasoft.archidata.annotation.method.RESTORE;
import org.glassfish.jersey.media.multipart.FormDataParam;

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
		final Annotation[] annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element, Path.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((Path) annotation[0]).value();
	}

	public static List<String> apiAnnotationProduces(final Class<?> element) throws Exception {
		final Annotation[] annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element, Produces.class);
		if (annotation.length == 0) {
			return null;
		}
		return Arrays.asList(((Produces) annotation[0]).value());
	}

	public static List<String> apiAnnotationProduces(final Method element) throws Exception {
		final Annotation[] annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element, Produces.class);
		if (annotation.length == 0) {
			return null;
		}
		return Arrays.asList(((Produces) annotation[0]).value());
	}

	public static boolean apiAnnotationTypeScriptProgress(final Method element) throws Exception {
		final Annotation[] annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element,
				ApiTypeScriptProgress.class);
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
		final List<Operation> annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element, Operation.class);
		if (annotation.size() == 0) {
			return null;
		}
		return annotation.get(0).description();
	}

	public static String apiAnnotationGetPath(final Method element) throws Exception {
		final List<Path> annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element, Path.class);
		if (annotation.size() == 0) {
			return null;
		}
		return annotation.get(0).value();
	}

	public static String apiAnnotationGetTypeRequest(final Method element) throws Exception {
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, GET.class).length == 1) {
			return "GET";
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, POST.class).length == 1) {
			return "POST";
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, PUT.class).length == 1) {
			return "PUT";
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, PATCH.class).length == 1) {
			return "PATCH";
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, DELETE.class).length == 1) {
			return "DELETE";
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, RESTORE.class).length == 1) {
			return "RESTORE";
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, ARCHIVE.class).length == 1) {
			return "ARCHIVE";
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, CALL.class).length == 1) {
			return "CALL";
		}
		return null;
	}

	public static RestTypeRequest apiAnnotationGetTypeRequest2(final Method element) throws Exception {
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, GET.class).length == 1) {
			return RestTypeRequest.GET;
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, POST.class).length == 1) {
			return RestTypeRequest.POST;
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, PUT.class).length == 1) {
			return RestTypeRequest.PUT;
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, PATCH.class).length == 1) {
			return RestTypeRequest.PATCH;
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, DELETE.class).length == 1) {
			return RestTypeRequest.DELETE;
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, RESTORE.class).length == 1) {
			return RestTypeRequest.RESTORE;
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, ARCHIVE.class).length == 1) {
			return RestTypeRequest.ARCHIVE;
		}
		if (AnnotationTools.getAnnotationsIncludingInterfaces(element, CALL.class).length == 1) {
			return RestTypeRequest.CALL;
		}
		return null;
	}

	public static String apiAnnotationGetPathParam(final Parameter element) throws Exception {
		final Annotation[] annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element, PathParam.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((PathParam) annotation[0]).value();
	}

	public static String apiAnnotationGetQueryParam(final Parameter element) throws Exception {
		final Annotation[] annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element, QueryParam.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((QueryParam) annotation[0]).value();
	}

	public static boolean apiAnnotationGetApiInputOptional(final Parameter element) throws Exception {
		final Annotation[] annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element,
				ApiInputOptional.class);
		if (annotation.length == 0) {
			return false;
		}
		return true;
	}

	public static String apiAnnotationGetFormDataParam(final Parameter element) throws Exception {
		final Annotation[] annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element, FormDataParam.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((FormDataParam) annotation[0]).value();
	}

	public static Class<?>[] apiAnnotationGetAsyncType(final Parameter element) throws Exception {
		final Annotation[] annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element, ApiAsyncType.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((ApiAsyncType) annotation[0]).value();
	}

	public static Class<?>[] apiAnnotationGetAsyncType(final Method element) throws Exception {
		final Annotation[] annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element, ApiAsyncType.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((ApiAsyncType) annotation[0]).value();
	}

	public static List<String> apiAnnotationGetConsumes(final Method element) throws Exception {
		final Annotation[] annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element, Consumes.class);
		if (annotation.length == 0) {
			return null;
		}
		return Arrays.asList(((Consumes) annotation[0]).value());
	}

	public static List<String> apiAnnotationGetConsumes(final Class<?> element) throws Exception {
		final Annotation[] annotation = AnnotationTools.getAnnotationsIncludingInterfaces(element, Consumes.class);
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
		return AnnotationTools.getAnnotationsIncludingInterfaces(element, Context.class).length != 0;
	}

}
