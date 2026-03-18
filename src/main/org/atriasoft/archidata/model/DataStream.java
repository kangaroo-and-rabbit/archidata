package org.atriasoft.archidata.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Describes a stream/track within a data file (video, audio, subtitle, image, chapter)")
public class DataStream {

	public enum StreamType {
		VIDEO, AUDIO, SUBTITLE, IMAGE, CHAPTER
	}

	// Common fields
	@Schema(description = "Type of the stream")
	private StreamType type;
	@Schema(description = "Codec identifier (e.g. AV1, H264, Opus, SRT, JPEG)")
	private String codec;
	@Schema(description = "Language code (e.g. fre, eng)")
	private String language;
	@Schema(description = "Duration in seconds")
	private Double duration;
	@Schema(description = "Track name if available")
	private String name;

	// Video & Image fields
	@Schema(description = "Width in pixels")
	private Integer width;
	@Schema(description = "Height in pixels")
	private Integer height;
	@Schema(description = "Frame rate (video only)")
	private Double frameRate;

	// Audio fields
	@Schema(description = "Sample rate in Hz (audio only)")
	private Integer sampleRate;
	@Schema(description = "Number of audio channels")
	private Integer channels;
	@Schema(description = "Bit depth (audio only)")
	private Integer bitDepth;

	// Chapter fields
	@Schema(description = "Start time in seconds (chapter only)")
	private Double startTime;
	@Schema(description = "End time in seconds (chapter only)")
	private Double endTime;
	@Schema(description = "Chapter title")
	private String title;

	public DataStream() {}

	public StreamType getType() {
		return this.type;
	}

	public void setType(final StreamType type) {
		this.type = type;
	}

	public String getCodec() {
		return this.codec;
	}

	public void setCodec(final String codec) {
		this.codec = codec;
	}

	public String getLanguage() {
		return this.language;
	}

	public void setLanguage(final String language) {
		this.language = language;
	}

	public Double getDuration() {
		return this.duration;
	}

	public void setDuration(final Double duration) {
		this.duration = duration;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Integer getWidth() {
		return this.width;
	}

	public void setWidth(final Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return this.height;
	}

	public void setHeight(final Integer height) {
		this.height = height;
	}

	public Double getFrameRate() {
		return this.frameRate;
	}

	public void setFrameRate(final Double frameRate) {
		this.frameRate = frameRate;
	}

	public Integer getSampleRate() {
		return this.sampleRate;
	}

	public void setSampleRate(final Integer sampleRate) {
		this.sampleRate = sampleRate;
	}

	public Integer getChannels() {
		return this.channels;
	}

	public void setChannels(final Integer channels) {
		this.channels = channels;
	}

	public Integer getBitDepth() {
		return this.bitDepth;
	}

	public void setBitDepth(final Integer bitDepth) {
		this.bitDepth = bitDepth;
	}

	public Double getStartTime() {
		return this.startTime;
	}

	public void setStartTime(final Double startTime) {
		this.startTime = startTime;
	}

	public Double getEndTime() {
		return this.endTime;
	}

	public void setEndTime(final Double endTime) {
		this.endTime = endTime;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return "DataStream [type=" + this.type + ", codec=" + this.codec + ", language=" + this.language + "]";
	}
}
