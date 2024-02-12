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

/** Body writer use in jersey with : In your main: ```java rc.register(new CSVMessageBodyWritter()); ```
 *
 * and in the produce element: ```java
 * @GET
 * @Produces(CSVMessageBodyWritter.CSV_TYPE) public List<Data> getData() {} ``` */
@Provider
@Produces("text/csv")
public class CSVMessageBodyWriter implements MessageBodyWriter<List<Object>> {
	public static final String CSV_TYPE = "text/csv";

	@Override
	public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
		return List.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(final List<Object> data, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
		return 0;
	}

	@Override
	public void writeTo(final List<Object> data, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType,
			final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream) throws IOException, WebApplicationException {
		if (data != null && data.size() > 0) {
			final CsvMapper mapper = new CsvMapper();
			final Object o = data.get(0);
			final CsvSchema schema = mapper.schemaFor(o.getClass()).withHeader();
			mapper.writer(schema).writeValue(entityStream, data);
		}
	}
}
