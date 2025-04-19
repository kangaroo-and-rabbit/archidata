package test.atriasoft.archidata.apiExtern.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataForJSR310 {
	public LocalTime localTime;
	public LocalDate localDate;
	public Date date;
	public LocalDateTime localDateTime;
	public ZonedDateTime zoneDateTime;

	@Override
	public String toString() {
		return "DataForJSR310 [localTime=" + this.localTime + ", localDate=" + this.localDate + ", localDateTime="
				+ this.localDateTime + ", zoneDateTime=" + this.zoneDateTime + "]";
	}
}
