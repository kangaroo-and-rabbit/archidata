package test.kar.archidata.apiExtern.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DataForJSR310 {
	public LocalTime time;
	public LocalDate date;
	public LocalDateTime dateTime;

	@Override
	public String toString() {
		return "DataForJSR310 [time=" + this.time + ", date=" + this.date + ", dateTime=" + this.dateTime + "]";
	}

}
