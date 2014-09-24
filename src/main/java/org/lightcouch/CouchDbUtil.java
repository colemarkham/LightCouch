/*
 * Copyright (C) 2011 lightcouch.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lightcouch;

import static java.lang.String.format;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Scanner;
import java.util.UUID;

import org.apache.http.HttpResponse;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Provides various utility methods, for internal use.
 * @author Ahmed Yehia
 */
final class CouchDbUtil {

	private CouchDbUtil() {
		// Utility class
	}
	
	public static void assertNotEmpty(Object object, String prefix) throws IllegalArgumentException {
		if(object == null) {
			throw new IllegalArgumentException(format("%s may not be null.", prefix));
		} else if(object instanceof String && ((String)object).length() == 0) {
			throw new IllegalArgumentException(format("%s may not be empty.", prefix));
		} 
	}
	
	public static void assertNull(Object object, String prefix) throws IllegalArgumentException {
		if(object != null) {
			throw new IllegalArgumentException(format("%s should be null.", prefix));
		} 
	}
	
	public static String generateUUID() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	// JSON
	
	public static <T> T JsonToObject(Gson gson, JsonElement elem, String key, Class<T> classType) {
		return gson.fromJson(elem.getAsJsonObject().get(key), classType);
	}

	/**
	 * @return A JSON element as a String, or null if not found.
	 */
	public static String getAsString(JsonObject j, String e) {
		return (j.get(e) == null) ? null : j.get(e).getAsString();  
	}
	
	/**
	 * @return A JSON element as <code>long</code>, or <code>0</code> if not found.
	 */
	public static long getAsLong(JsonObject j, String e) {
		return (j.get(e) == null) ? 0L : j.get(e).getAsLong();
	}
	
	/**
	 * @return A JSON element as <code>int</code>, or <code>0</code> if not found.
	 */
	public static int getAsInt(JsonObject j, String e) {
		return (j.get(e) == null) ? 0 : j.get(e).getAsInt();
	}
	
	/**
	 * @return {@link InputStream} of {@link HttpResponse}
	 */
	public static InputStream getStream(HttpResponse response) {
		try { 
			return response.getEntity().getContent();
		} catch (Exception e) {
			throw new CouchDbException("Error reading response. ", e);
		}
	}
	
	public static String removeExtension(String fileName) {
		return fileName.substring(0, fileName.lastIndexOf('.'));
	}
	
	public static String streamToString(InputStream in) {
	    Scanner s = new Scanner(in);
	    s.useDelimiter("\\A");
	    String str = s.hasNext() ? s.next() : null;
	    close(in);
	    close(s);
	    return str;
	}
	
	/**
	 * Closes the response input stream.
	 * 
	 * @param response The {@link HttpResponse}
	 */
	public static void close(HttpResponse response) {
		try {
			close(response.getEntity().getContent());
		} catch (Exception e) {}
	}
	
	/**
	 * Closes a resource.
	 * 
	 * @param c The {@link Closeable} resource.
	 */
	public static void close(Closeable c) {
		try {
			c.close();
		} catch (Exception e) {}
	}
}
