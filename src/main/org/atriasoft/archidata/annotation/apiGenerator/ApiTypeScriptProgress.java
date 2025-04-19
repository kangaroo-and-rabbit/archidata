package org.atriasoft.archidata.annotation.apiGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The TypeScriptProgress annotation is used to specify that an API method
 * will take a significant amount of time to complete, and thus requires a
 * callback API to provide more precise progress tracking, particularly for
 * upload operations.
 *
 * <p>Usage:
 * - Target: This annotation can be applied to method parameters and methods.
 * - Retention: The annotation is retained at runtime, allowing it to be
 *   processed by frameworks or libraries that handle code generation logic.
 *
 * <p>Behavior:
 * - When applied to a method or parameter, the TypeScriptProgress annotation
 *   indicates that the client code generator should provide a callback API
 *   for tracking the progress of the operation.
 * - Note: The use of this annotation implies that the standard browser fetch
 *   API is not used, as the callback API is not yet operational. Instead,
 *   the older XMLHttpRequest interface is utilized.
 *
 * <p>Example:
 * {@code
 * public class SeasonService {
 *
 *     @POST
 *     @Path("{id}/cover")
 *     @RolesAllowed("ADMIN")
 *     @Consumes(MediaType.MULTIPART_FORM_DATA)
 *     @Operation(description = "Upload a new season cover season", tags = "GLOBAL")
 *     @TypeScriptProgress
 *     public Season uploadCover(@PathParam("id") final Long id,
 *                               @FormDataParam("file") final InputStream fileInputStream,
 *                               @FormDataParam("file") final FormDataContentDisposition fileMetaData)
 *             throws Exception {
 *         // Upload logic
 *     }
 * }
 * }
 *
 * In this example, the uploadCover method will generate a client-side API
 * with progress tracking capabilities using XMLHttpRequest.
 *
 * <p>Generated TypeScript code example:
 * {@code
 * export function uploadCover({
 *        restConfig,
 *        params,
 *        data,
 *        callbacks, // add this callback handle
 *    }: {
 *    restConfig: RESTConfig,
 *    params: {
 *        id: Long,
 *    },
 *    data: {
 *        file: File,
 *    },
 *    callbacks?: RESTCallbacks,
 * }): Promise<Season> {...}
 * }
 *
 *
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiTypeScriptProgress {}
