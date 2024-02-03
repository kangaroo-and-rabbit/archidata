package org.kar.archidata.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

/**
 * Body writter use in jersey with :
 * In your main:
 * ```java
 * rc.register(new CSVMessageBodyWritter());
 * ```
 *
 * and in the produce element:
 * ```java
 * @GET
 * @Produces("text/csv")
 * public List<Data> getData() {}
 * ```
 */
@Provider
@Produces("text/csv")
public class CSVMessageBodyWritter implements MessageBodyWriter<List<?>> {

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return List.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(List<?> data, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return 0;
	}

	@Override
	public void writeTo(List<?> data, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
		if (data != null && data.size() > 0) {
			CsvMapper mapper = new CsvMapper();
			Object o = data.get(0);
			CsvSchema schema = mapper.schemaFor(o.getClass()).withHeader();
			mapper.writer(schema).writeValue(entityStream, data);
		}
	}
}
