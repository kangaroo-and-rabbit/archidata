package org.kar.archidata.checker;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.Priority;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(1000) // Définit la priorité d'exécution du filtre
public class ValidationFilter implements ContainerRequestFilter {

	private final Map<Class<?>, CheckJPA<?>> allocatedChecker = new HashMap<>();

	@Context
	private ResourceInfo resourceInfo;
	@Context
	private UriInfo uriInfo;

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {
		final Method method = this.resourceInfo.getResourceMethod();
		if (method == null) {
			return;
		}
		final boolean hasValidAnnotation = Arrays.stream(method.getParameters())
				.anyMatch(param -> param.isAnnotationPresent(Valid.class));
		for (final Parameter parameter : method.getParameters()) {
			if (parameter.isAnnotationPresent(Valid.class)) {
				final Class<?> paramType = parameter.getType();
				CheckJPA<?> elem = this.allocatedChecker.get(paramType);
				if (elem == null) {
					Constructor<?> constructor;
					try {
						constructor = CheckJPA.class.getConstructor(Class.class);
						elem = (CheckJPA<?>) constructor.newInstance(paramType);
					} catch (NoSuchMethodException | SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (final InstantiationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (final IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (final IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (final InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					this.allocatedChecker.put(paramType, elem);
				}
				if (elem == null) {
					return;
				}
				final Object value = getParameterValue(parameter, requestContext, paramType);
				if (value == null) {
					// TODO: faire un choix ...

					throw new BadRequestException("Le paramètre validé ne peut pas être null.");
				}
				try {
					// TODO: il faut récupérer ici dans quel condition on est "Update" "Create" ou autre ...
					// TODO: Il faut surement faire un double element de vérification genre BDD et API ...
					elem.check(value);
				} catch (final Exception e) {
					e.printStackTrace();
					// TODO Auto-generated catch block
					throw new IOException("Fail to check ... ", e);
				}
				System.out.println("Valeur du paramètre @Valid : " + value);
			}
		}
		if (hasValidAnnotation) {
			throw new BadRequestException("La requête doit contenir un paramètre annoté avec @Valid");
		}
	}

	private Object getParameterValue(
			final Parameter parameter,
			final ContainerRequestContext requestContext,
			final Class<?> paramType) {
		// Vérification des paramètres de requête (QueryParam)
		final MultivaluedMap<String, String> queryParams = this.uriInfo.getQueryParameters();
		if (queryParams.containsKey(parameter.getName())) {
			return queryParams.getFirst(parameter.getName());
		}

		// Vérification du corps de la requête (JSON)
		if (requestContext.hasEntity()) {
			try {
				final ObjectMapper objectMapper = new ObjectMapper();
				return objectMapper.readValue(requestContext.getEntityStream(), paramType);
			} catch (final Exception e) {
				throw new BadRequestException(
						"Erreur de lecture du corps de la requête pour le type " + paramType.getName());
			}
		}

		return null;
	}
}