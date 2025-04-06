package test.kar.archidata.apiExtern.resource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

import org.kar.archidata.tools.DateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import test.kar.archidata.apiExtern.model.DataForJSR310;

@Path("/TimeResource")
@Produces({ MediaType.APPLICATION_JSON })
public class TimeResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(TimeResource.class);

	@POST
	@PermitAll
	public DataForJSR310 post(final DataForJSR310 data) throws Exception {
		LOGGER.warn("receive Data: {}", data);
		return data;
	}

	@POST
	@Path("serialize")
	@PermitAll
	public String serialize(final DataForJSR310 data) throws Exception {
		LOGGER.warn("receive Data: {}", data);
		if (data.localDate != null) {
			return data.localDate.toString();
		}
		if (data.localDateTime != null) {
			return data.localDateTime.toString();
		}
		if (data.localTime != null) {
			return data.localTime.toString();
		}
		if (data.zoneDateTime != null) {
			return data.zoneDateTime.toString();
		}
		if (data.date != null) {
			return data.date.toString();
		}
		return "";
	}

	@POST
	@Path("unserialize")
	@PermitAll
	public DataForJSR310 unserialize(final String data) throws Exception {
		LOGGER.warn("receive Data: {}", data);
		final DataForJSR310 newData = new DataForJSR310();
		try {
			newData.localDate = LocalDate.parse(data);
		} catch (final Exception ex) {}
		try {
			newData.localDateTime = LocalDateTime.parse(data);
		} catch (final Exception ex) {}
		try {
			newData.date = DateTools.parseDate(data);
			//			final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
			//			final OffsetDateTime dateTime = OffsetDateTime.parse(data, formatter);
			//			final Instant instant = dateTime.toInstant();
			//			newData.date = Date.from(instant);
		} catch (final Exception ex) {}
		try {
			newData.localTime = LocalTime.parse(data);
		} catch (final Exception ex) {}
		try {
			newData.zoneDateTime = ZonedDateTime.parse(data);
		} catch (final Exception ex) {}
		return newData;
	}

	@GET
	@PermitAll
	@Path("inputDate")
	public String postInputDate(@QueryParam("date") final Date data) throws Exception {
		return DateTools.serializeNanoWithUTCTimeZone(data);
	}

	@GET
	@PermitAll
	@Path("inputOffsetDateTime")
	public String postInputOffsetDateTime(@QueryParam("date") final OffsetDateTime data) throws Exception {
		return DateTools.serializeNanoWithUTCTimeZone(data);
	}
}
