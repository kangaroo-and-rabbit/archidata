package org.kar.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The FormDataOptional annotation is used to indicate that a form data parameter
 * is optional when generating client code. By default, form data parameters are
 * required, but this annotation allows them to be optional, enabling the creation
 * of polymorphic APIs.
 *
 * <p>Usage:
 * - Target: This annotation can be applied to method parameters.
 * - Retention: The annotation is retained at runtime, allowing it to be
 *   processed by frameworks or libraries that handle code generation logic.
 *
 * <p>Behavior:
 * - When applied to a parameter, the FormDataOptional annotation specifies that
 *   the parameter is optional in the generated client code. This allows for
 *   more flexible API designs where certain inputs can be omitted.
 *
 * <p>Example:
 * <pre>{@code
 * public class AlbumService {
 *
 *     @POST
 *     @Path("{id}/cover")
 *     @RolesAllowed("ADMIN")
 *     @Consumes({ MediaType.MULTIPART_FORM_DATA })
 *     @Operation(description = "Add a cover on a specific album")
 *     @TypeScriptProgress
 *     public Album uploadCover(@PathParam("id") final Long id,
 *                              @FormDataOptional @FormDataParam("uri") final String uri,
 *                              @FormDataOptional @FormDataParam("file") final InputStream fileInputStream,
 *                              @FormDataOptional @FormDataParam("file") final FormDataContentDisposition fileMetaData)
 *             throws Exception {
 *         // some code
 *     }
 * }
 * }</pre>
 *
 * Note: @FormDataParam must be allway at the last position.
 *
 * In this example, the uri, fileInputStream, and fileMetaData parameters are
 * marked as optional, allowing the client to omit them when calling the API.
 *
 * <p>Generated TypeScript code example:
 * <pre>{@code
 * //Add a cover on a specific album
 * export function uploadCover({
 *     restConfig,
 *     params,
 *     data,
 *     callbacks,
 * }: {
 *     restConfig: RESTConfig,
 *     params: {
 *         id: Long,
 *     },
 *     data: {
 *         file?: File, // element is optional
 *         uri?: string, // element is optional
 *     },
 *     callbacks?: RESTCallbacks,
 * }): Promise<Album> { ...
 * }</pre>
 *
 * The generated TypeScript function reflects the optional nature of the form data parameters.
 */
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface FormDataOptional {

}
