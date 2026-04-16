package org.atriasoft.archidata.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Describes a stream or track within a data file, such as video, audio, subtitle, image, or chapter tracks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Describes a stream/track within a data file (video, audio, subtitle, image, chapter)")
public class DataStream {

	/**
	 * Enumeration of supported stream types.
	 */
	public enum StreamType {
		/** Video stream. */
		VIDEO,
		/** Audio stream. */
		AUDIO,
		/** Subtitle stream. */
		SUBTITLE,
		/** Image stream. */
		IMAGE,
		/** Chapter marker. */
		CHAPTER
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

	/**
	 * Default constructor.
	 */
	public DataStream() {}

	/**
	 * Gets the type of this stream.
	 * @return the stream type
	 */
	public StreamType getType() {
		return this.type;
	}

	/**
	 * Sets the type of this stream.
	 * @param type the stream type to set
	 */
	public void setType(final StreamType type) {
		this.type = type;
	}

	/**
	 * Gets the codec identifier.
	 * @return the codec string (e.g. AV1, H264, Opus)
	 */
	public String getCodec() {
		return this.codec;
	}

	/**
	 * Sets the codec identifier.
	 * @param codec the codec string to set
	 */
	public void setCodec(final String codec) {
		this.codec = codec;
	}

	/**
	 * Gets the language code.
	 * @return the language code (e.g. fre, eng)
	 */
	public String getLanguage() {
		return this.language;
	}

	/**
	 * Sets the language code.
	 * @param language the language code to set
	 */
	public void setLanguage(final String language) {
		this.language = language;
	}

	/**
	 * Gets the duration in seconds.
	 * @return the duration in seconds
	 */
	public Double getDuration() {
		return this.duration;
	}

	/**
	 * Sets the duration in seconds.
	 * @param duration the duration in seconds to set
	 */
	public void setDuration(final Double duration) {
		this.duration = duration;
	}

	/**
	 * Gets the track name.
	 * @return the track name, or {@code null} if not available
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the track name.
	 * @param name the track name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Gets the width in pixels (video and image streams).
	 * @return the width in pixels
	 */
	public Integer getWidth() {
		return this.width;
	}

	/**
	 * Sets the width in pixels.
	 * @param width the width in pixels to set
	 */
	public void setWidth(final Integer width) {
		this.width = width;
	}

	/**
	 * Gets the height in pixels (video and image streams).
	 * @return the height in pixels
	 */
	public Integer getHeight() {
		return this.height;
	}

	/**
	 * Sets the height in pixels.
	 * @param height the height in pixels to set
	 */
	public void setHeight(final Integer height) {
		this.height = height;
	}

	/**
	 * Gets the frame rate (video streams only).
	 * @return the frame rate in frames per second
	 */
	public Double getFrameRate() {
		return this.frameRate;
	}

	/**
	 * Sets the frame rate.
	 * @param frameRate the frame rate in frames per second to set
	 */
	public void setFrameRate(final Double frameRate) {
		this.frameRate = frameRate;
	}

	/**
	 * Gets the sample rate in Hz (audio streams only).
	 * @return the sample rate in Hz
	 */
	public Integer getSampleRate() {
		return this.sampleRate;
	}

	/**
	 * Sets the sample rate in Hz.
	 * @param sampleRate the sample rate in Hz to set
	 */
	public void setSampleRate(final Integer sampleRate) {
		this.sampleRate = sampleRate;
	}

	/**
	 * Gets the number of audio channels.
	 * @return the number of channels
	 */
	public Integer getChannels() {
		return this.channels;
	}

	/**
	 * Sets the number of audio channels.
	 * @param channels the number of channels to set
	 */
	public void setChannels(final Integer channels) {
		this.channels = channels;
	}

	/**
	 * Gets the bit depth (audio streams only).
	 * @return the bit depth
	 */
	public Integer getBitDepth() {
		return this.bitDepth;
	}

	/**
	 * Sets the bit depth.
	 * @param bitDepth the bit depth to set
	 */
	public void setBitDepth(final Integer bitDepth) {
		this.bitDepth = bitDepth;
	}

	/**
	 * Gets the start time in seconds (chapter streams only).
	 * @return the start time in seconds
	 */
	public Double getStartTime() {
		return this.startTime;
	}

	/**
	 * Sets the start time in seconds.
	 * @param startTime the start time in seconds to set
	 */
	public void setStartTime(final Double startTime) {
		this.startTime = startTime;
	}

	/**
	 * Gets the end time in seconds (chapter streams only).
	 * @return the end time in seconds
	 */
	public Double getEndTime() {
		return this.endTime;
	}

	/**
	 * Sets the end time in seconds.
	 * @param endTime the end time in seconds to set
	 */
	public void setEndTime(final Double endTime) {
		this.endTime = endTime;
	}

	/**
	 * Gets the chapter title.
	 * @return the chapter title
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Sets the chapter title.
	 * @param title the chapter title to set
	 */
	public void setTitle(final String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return "DataStream [type=" + this.type + ", codec=" + this.codec + ", language=" + this.language + "]";
	}
}
