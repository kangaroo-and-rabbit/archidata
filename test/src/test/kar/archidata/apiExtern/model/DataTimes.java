package test.kar.archidata.apiExtern.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

public class DataTimes {
	public Date time;
	public Timestamp date;
	public LocalDateTime dateTime;

	@Override
	public String toString() {
		return "DataForJSR310 [time=" + this.time + ", date=" + this.date + ", dateTime=" + this.dateTime + "]";
	}

}
