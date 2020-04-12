package com.csc301.profilemicroservice;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.neo4j.driver.v1.Transaction;
import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}
	
	
	// NEED TO IMPLEMENT ERROR CHECKING 
	@Override
	public DbQueryStatus likeSong(String userName, String songId) {
		OkHttpClient client = new OkHttpClient();
		
		try (Session session = driver.session())
        {
            try (Transaction trans = session.beginTransaction())
            {
                trans.run("MATCH (pl:playlist) WHERE pl.plName = ({x1} + '-favorites playlist') MERGE (s:song {songId: {x2}, sUserName: {x1}}) CREATE UNIQUE (pl)-[:includes]->(s)", parameters("x1", userName, "x2", songId));
                trans.success(); 
                
            }catch(Exception e) {
            	
            	DbQueryStatus status = new DbQueryStatus("Transaction did not start", DbQueryExecResult.QUERY_ERROR_GENERIC);
            	return status;
            }
        }catch(Exception e) {
        	
        	DbQueryStatus status = new DbQueryStatus("Session did not start", DbQueryExecResult.QUERY_ERROR_GENERIC);
        	return status;
        }
		
		// The below block of code sends a request to Song Microservice to increase song like counter by 1
		
		HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/updateSongFavouritesCount/" + songId).newBuilder();
		urlBuilder.addQueryParameter("shouldDecrement", "true");
		String url = urlBuilder.build().toString();

	    RequestBody body = RequestBody.create(null, new byte[0]);

		Request request = new Request.Builder()
				.url(url)
				.method("PUT", body)
				.build();

		Call call = client.newCall(request);
		Response responseFromAddMs = null;

		String addServiceBody = "{}";

		try {
			responseFromAddMs = call.execute();
			addServiceBody = responseFromAddMs.body().string();
			//response.put("data", mapper.readValue(addServiceBody, Map.class));
		} catch (IOException e) {
			DbQueryStatus status = new DbQueryStatus("Request call not executed to Song Microservice", DbQueryExecResult.QUERY_ERROR_GENERIC);
        	return status;
		}
		DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		return status;
	}

	
	// NEED TO IMPLEMENT ERROR CHECKING
	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		OkHttpClient client = new OkHttpClient();
		
		try (Session session = driver.session())
        {
            try (Transaction trans = session.beginTransaction())
            {
                trans.run("MATCH (p:song {songId: {x1}, sUserName: {x2}}) DETACH DELETE p", parameters("x1", songId, "x2", userName));
                trans.success(); 
            }catch(Exception e) {
            	
            	DbQueryStatus status = new DbQueryStatus("Transaction did not start", DbQueryExecResult.QUERY_ERROR_GENERIC);
            	return status;
            }
        }catch(Exception e) {
        	
        	DbQueryStatus status = new DbQueryStatus("Session did not start", DbQueryExecResult.QUERY_ERROR_GENERIC);
        	return status;
        }
		// The below block of code sends a request to Song Microservice to decreases song like counter by 1
		
		HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/updateSongFavouritesCount/" + songId).newBuilder();
		urlBuilder.addQueryParameter("shouldDecrement", "false");
		String url = urlBuilder.build().toString();

	    RequestBody body = RequestBody.create(null, new byte[0]);

		Request request = new Request.Builder()
				.url(url)
				.method("PUT", body)
				.build();

		Call call = client.newCall(request);
		Response responseFromAddMs = null;

		String addServiceBody = "{}";

		try {
			responseFromAddMs = call.execute();
			addServiceBody = responseFromAddMs.body().string();
		} catch (IOException e) {
			DbQueryStatus status = new DbQueryStatus("Request call not executed to Song Microservice", DbQueryExecResult.QUERY_ERROR_GENERIC);
        	return status;
		}
		DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		return status;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		try (Session session = driver.session())
        {
            try (Transaction trans = session.beginTransaction())
            {
                trans.run("MATCH (p:song {songId: {x1}}) DETACH DELETE p", parameters("x1", songId));
                trans.success(); 
                DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
            	return status;
            }catch(Exception e) {
            	
            	DbQueryStatus status = new DbQueryStatus("Transaction did not start", DbQueryExecResult.QUERY_ERROR_GENERIC);
            	return status;
            }
        }catch(Exception e) {
        	
        	DbQueryStatus status = new DbQueryStatus("Session did not start", DbQueryExecResult.QUERY_ERROR_GENERIC);
        	return status;
        }
	}
}
