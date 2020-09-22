package io.awesome.gagtube.download.helper;

import java.io.Serializable;
import java.util.Calendar;

import androidx.annotation.NonNull;
import io.awesome.gagtube.download.io.StoredFileHelper;

public abstract class Mission implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Source url of the resource
	 */
	public String source;
	
	/**
	 * Length of the current resource
	 */
	public long length;
	
	/**
	 * creation timestamp (and maybe unique identifier)
	 */
	public long timestamp;
	
	/**
	 * pre-defined content type
	 */
	public char kind;
	
	/**
	 * The downloaded file
	 */
	public StoredFileHelper storage;
	
	/**
	 * Delete the downloaded file
	 *
	 * @return {@code true] if and only if the file is successfully deleted, otherwise, {@code false}
	 */
	public boolean delete() {
		if (storage != null) return storage.delete();
		return true;
	}
	
	/**
	 * Indicate if this mission is deleted whatever is stored
	 */
	public transient boolean deleted = false;
	
	@NonNull
	@Override
	public String toString() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timestamp);
		return "[" + calendar.getTime().toString() + "] " + (storage.isInvalid() ? storage.getName() : storage.getUri());
	}
}
