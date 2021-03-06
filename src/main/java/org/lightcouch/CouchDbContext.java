/*
 * Copyright (C) lightcouch.org
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

import static org.lightcouch.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.CouchDbUtil.close;
import static org.lightcouch.CouchDbUtil.getAsString;
import static org.lightcouch.URIBuilder.buildUri;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/**
 * Contains database server specific APIs.
 * 
 * @see CouchDbClient#context() 
 * @since 0.0.2
 * @author Ahmed Yehia
 */
public class CouchDbContext {

	private static final Log log = LogFactory.getLog(CouchDbClient.class);

	private CouchDbClientBase dbc;

   private CouchDbProperties props;
   
   private ResourceProvider resourceProvider = new ClasspathResourceProvider();

	CouchDbContext(CouchDbClientBase dbc, CouchDbProperties props) {
		this.dbc = dbc;
      this.props = props;
		if (props.isCreateDbIfNotExist()) {
			createDB(props.getDbName());
		} else {
			serverVersion(); // pre warm up client
		}
	}

	public void setResourceProvider(ResourceProvider resourceProvider){
      this.resourceProvider = resourceProvider;
   }
	
	public ResourceProvider getResourceProvider(){
      return resourceProvider;
   }

	/**
	 * Requests CouchDB deletes a database.
	 * @param dbName The database name
	 * @param confirm A confirmation string with the value: <tt>delete database</tt>
	 */
	public void deleteDB(String dbName, String confirm) {
		assertNotEmpty(dbName, "dbName");
		if(!"delete database".equals(confirm))
			throw new IllegalArgumentException("Invalid confirm!");
		dbc.delete(buildUri(dbc.getBaseUri()).path(dbName).build());
	}

	/**
	 * Requests CouchDB creates a new database; if one doesn't exist.
	 * @param dbName The Database name
	 */
	public void createDB(String dbName) {
		assertNotEmpty(dbName, "dbName");
		InputStream getresp = null;
		HttpResponse putresp = null;
		final URI uri = buildUri(dbc.getBaseUri()).path(dbName).build();
		try {
			getresp = dbc.get(uri);
		} catch (NoDocumentException e) { // db doesn't exist
			final HttpPut put = new HttpPut(uri);
			putresp = dbc.executeRequest(put);
			log.info(String.format("Created Database: '%s'", dbName));
		} finally {
			close(getresp);
			close(putresp);
		}
	}

	/**
	 * @return All Server databases.
	 */
	public List<String> getAllDbs() {
		InputStream instream = null;
		try {
			Type typeOfList = new TypeToken<List<String>>() {}.getType();
			instream = dbc.get(buildUri(dbc.getBaseUri()).path("_all_dbs").build());
			Reader reader = new InputStreamReader(instream);
			return dbc.getGson().fromJson(reader, typeOfList);
		} finally {
			close(instream);
		}
	}
	
	/**
	 * Creates a client with the same type and connection parameters as the one that this context belongs to.
	 * @param dbName
	 * @param createDbIfNotExist whether to create the database if it does not already exist
	 * @return The client for accessing the database. May be safely cast to the original client class.
	 * @throws CouchDbException 
	 */
	public CouchDbClientBase createDbClient(String dbName, boolean createDbIfNotExist) throws CouchDbException {
	   Class<? extends CouchDbClientBase> clientClass = dbc.getClass();
	   try{
         Constructor<? extends CouchDbClientBase> constructor = clientClass.getConstructor(CouchDbProperties.class);
         CouchDbProperties properties = new CouchDbProperties(props);
         properties.setDbName(dbName);
         properties.setCreateDbIfNotExist(createDbIfNotExist);
         return constructor.newInstance(properties);
      }catch(ReflectiveOperationException e){
         throw new CouchDbException("Unable to construct client class, " + dbc.getClass().getName() + ", for dbName: " + dbName, e);
      }
	}
	
	/**
	 * @return {@link CouchDbInfo} Containing the DB server info.
	 */
	public CouchDbInfo info() {
		return dbc.get(buildUri(dbc.getDBUri()).build(), CouchDbInfo.class);
	}

	/**
	 * @return DB Server version.
	 */
	public String serverVersion() {
		InputStream instream = null;
		try {
			instream = dbc.get(buildUri(dbc.getBaseUri()).build());
			Reader reader = new InputStreamReader(instream);
			return getAsString(new JsonParser().parse(reader).getAsJsonObject(), "version");
		} finally {
			close(instream);
		}
	}

	/**
	 * Triggers a database <i>compact</i> request.
	 */
	public void compact() {
		HttpResponse response = null;
		try {
			response = dbc.post(buildUri(dbc.getDBUri()).path("_compact").build(), "");
		} finally {
			close(response);
		}
	}

	/**
	 * Requests the database commits any recent changes to disk.
	 */
	public void ensureFullCommit() {
		HttpResponse response = null;
		try {
			response = dbc.post(buildUri(dbc.getDBUri()).path("_ensure_full_commit").build(), "");
		} finally {
			close(response);
		}
	}
	
	/**
	 * Request a database sends a list of UUIDs.
	 * @param count The count of UUIDs.
	 */
	public List<String> uuids(long count) {
		final String uri = String.format("%s_uuids?count=%d", dbc.getBaseUri(), count);
		final JsonObject json = dbc.findAny(JsonObject.class, uri);
		return dbc.getGson().fromJson(json.get("uuids").toString(), new TypeToken<List<String>>(){}.getType());
	}
}
