package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.*; 
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;


import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.neo4j.driver.v1.Transaction;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}
	
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		// Error checking.
		try {
			try (Session session = driver.session())
	        {
	            try (Transaction trans = session.beginTransaction())
	            {
	                trans.run("CREATE (p:profile {userName: {x1}, fullName: {x2}, password: {x3}}) - [:created] -> "
	                		+ "(pl:playlist {plName: {x1} + '-favorites playlist'})", parameters("x1", userName, "x2", fullName, "x3", password));
	                trans.success(); 
	                DbQueryStatus status = new DbQueryStatus("OK",DbQueryExecResult.QUERY_OK);
	                return status;
	            } catch(Exception e) {
	            	
	            	DbQueryStatus status = new DbQueryStatus("Transaction did not start", DbQueryExecResult.QUERY_ERROR_GENERIC);
	            	return status;
	            }
	        } catch (Exception e) {
	        	DbQueryStatus status = new DbQueryStatus("Driver Session did not begin", DbQueryExecResult.QUERY_ERROR_GENERIC);
	        	return status;
	        }
			
		} catch (Exception e) {
			DbQueryStatus status = new DbQueryStatus("Unknown error: Profile not created", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        	return status;
		}  
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		// Error checking.
		try {
			try (Session session = driver.session())
	        {
	            try (Transaction trans = session.beginTransaction())
	            {
	            	trans.run("MATCH (p:profile),(f:profile) WHERE p.userName = {x1} AND f.userName = {x2} CREATE (p)-[r:follows]->(f)", parameters("x1", userName, "x2", frndUserName));
	                trans.success();  
	                DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
	                return status;
	            } catch(Exception e) {
	            	
	            	DbQueryStatus status = new DbQueryStatus("Transaction did not start", DbQueryExecResult.QUERY_ERROR_GENERIC);
	            	return status;
	            }
	        } catch (Exception e) {
	        	DbQueryStatus status = new DbQueryStatus("Driver Session did not begin", DbQueryExecResult.QUERY_ERROR_GENERIC);
	        	return status;
	        }
			
		} catch (Exception e) {
			DbQueryStatus status = new DbQueryStatus("Unknown error", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        	return status;
		}
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		// Error checking.
		try {
			try (Session session = driver.session())
	        {
	            try (Transaction trans = session.beginTransaction())
	            {
	            	trans.run("MATCH (p { userName: {x1} })-[r:follows]->(f { userName: {x2} }) DELETE r", parameters("x1", userName, "x2", frndUserName));
	                trans.success();  
	                DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
	                return status;
	            } catch(Exception e) {
	            	
	            	DbQueryStatus status = new DbQueryStatus("Transaction did not start", DbQueryExecResult.QUERY_ERROR_GENERIC);
	            	return status;
	            }
	        } catch (Exception e) {
	        	DbQueryStatus status = new DbQueryStatus("Driver Session did not begin", DbQueryExecResult.QUERY_ERROR_GENERIC);
	        	return status;
	        }
			
		} catch (Exception e) {
			DbQueryStatus status = new DbQueryStatus("Unknown error", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        	return status;
		}	
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		OkHttpClient client = new OkHttpClient();
		try(Session session = driver.session()){
			try (Transaction trans = session.beginTransaction()){
				// okay first need to get all friends 
				StatementResult get_friends = trans.run("MATCH (p:profile {userName:{x1}})-[:follows]->(f:profile) RETURN f.userName", parameters("x1", userName));
				// Error checking to see if the user has friends.
				if(!get_friends.hasNext()) {
					DbQueryStatus status = new DbQueryStatus("User has no friends", DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				while(get_friends.hasNext()) {
					Record next = get_friends.next();
					String friend_name = next.get("f.userName").asString();
					String p_name = friend_name + "-favorites playlist";
					
					StatementResult get_songs = trans.run("MATCH (pl:playlist {plName:{x1}})-[:includes]->(s:song) RETURN s.songId", parameters("x1", p_name));
					// Error checking to see if the user friends has any songs.
					if(!get_songs.hasNext()) {
						DbQueryStatus status = new DbQueryStatus("User friend has no songs", DbQueryExecResult.QUERY_ERROR_GENERIC);
					}
					List<String> song_list = new ArrayList();
					// Here we will loop over get_songs, get songID and parse it to get the song name. Once done we will add that song title to song_list
					// arraylist and add that list alongside name to a json object 
					while(get_songs.hasNext()) {
						Record next2 = get_songs.next();
						String song_id = next2.get("s.songId").asString();
						
						// The below block of code sends a request to Song Microservice to get the song title given a sonbgId.
						HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/getSongTitleById/" + song_id).newBuilder();
						String url = urlBuilder.build().toString();

					    RequestBody body = RequestBody.create(null, new byte[0]);

						Request request = new Request.Builder()
								.url(url)
								.method("GET", null)
								.build();

						Call call = client.newCall(request);
						Response responseFromAddMs = null;

						String addServiceBody = "{}";

						try {
							responseFromAddMs = call.execute();
							addServiceBody = responseFromAddMs.body().string();
							//response.put("data", mapper.readValue(addServiceBody, Map.class));
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						JSONObject song_obj = new JSONObject(addServiceBody);
                       
                        String songTitle = song_obj.getString("data");
                        song_list.add(songTitle);
						
					}
					HashMap<String, List<String>> jsonRepresentation = new HashMap<String, List<String>>();
					JSONArray jsArray = new JSONArray(song_list);
					jsonRepresentation.put(friend_name, song_list);
					DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
					status.setData(jsonRepresentation);
					return status;
					
				}
			}
		}
		DbQueryStatus status = new DbQueryStatus("Something went wrong", DbQueryExecResult.QUERY_ERROR_GENERIC);
		return status;
	}
}
