package test.kar.archidata.apiExtern.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataForJSR310String {
	public String localTime;
	public String localDate;
	public String date;
	public String localDateTime;
	public String zoneDateTime;

	@Override
	public String toString() {
		return "DataForJSR310 [localTime=" + this.localTime + ", localDate=" + this.localDate + ", localDateTime="
				+ this.localDateTime + ", zoneDateTime=" + this.zoneDateTime + "]";
	}
}
