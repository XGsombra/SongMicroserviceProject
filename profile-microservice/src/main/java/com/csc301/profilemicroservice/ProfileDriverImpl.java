package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.springframework.stereotype.Repository;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
		if(userName == null || userName.isEmpty() || fullName == null || fullName.isEmpty() || password == null || password.isEmpty()) {
			return new DbQueryStatus("Parameter incorrect", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE (profile:profile{userName: \"" + userName + "\", fullName: \"" + fullName + "\", "
						+ "password: \"" + password + "\"}) ";
				trans.run(queryStr);

				queryStr = "CREATE (playlist:playlist{plName: \"" + userName + "-favorites\"}) ";
				trans.run(queryStr);

				queryStr = "MATCH (nProfile:profile), (nPlaylist:playlist) WHERE nProfile.userName = \"" + userName + "\" "
						+ "AND nPlaylist.plName = \""+userName+"-favorites\"" 
						+ "CREATE (nProfile)-[:created]->(nPlaylist)";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
			return new DbQueryStatus("Profile created successfully", DbQueryExecResult.QUERY_OK);
		} catch (Exception e500) {
			return new DbQueryStatus("Profile creation failed", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		if(userName.equals(frndUserName)) {
			return new DbQueryStatus("Cannot follow oneself", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "MATCH (nProfile:profile) WHERE nProfile.userName = \"" + userName + "\" RETURN nProfile";
				if (!trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("The user doesn't exit", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				};
				
				queryStr = "MATCH (nfProfile:profile) WHERE nfProfile.userName = \"" + frndUserName + "\" RETURN nfProfile";
				if (!trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("The friend doesn't exit", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				};
				
				queryStr = "MATCH (nProfile:profile)-[f:follows]->(nfProfile:profile) WHERE "
						+ "nProfile.userName = \"" + userName + "\" AND "
						+ "nfProfile.userName = \"" + frndUserName + "\" RETURN f";
				if (trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("Already followed", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				};
				
				queryStr = "MATCH (nProfile:profile), (nfProfile:profile) WHERE nProfile.userName = \"" + userName + "\" "
						+ "AND nfProfile.userName = \"" + frndUserName + "\""
						+ "CREATE (nProfile)-[:follows]->(nfProfile)";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
			return new DbQueryStatus("Friend followed successfully", DbQueryExecResult.QUERY_OK);
		} catch (Exception e500) {
			e500.printStackTrace();
			return new DbQueryStatus("Failed to follow friend", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		String queryStr;
		
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "MATCH (nProfile:profile) WHERE nProfile.userName = \"" + userName + "\" RETURN nProfile";
				if (!trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("The user doesn't exit", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				};
				
				queryStr = "MATCH (nfProfile:profile) WHERE nfProfile.userName = \"" + frndUserName + "\" RETURN nfProfile";
				if (!trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("The friend doesn't exit", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				};
				
				queryStr = "MATCH (nProfile:profile)-[r:follows]->(nfProfile:profile) WHERE nProfile.userName = \"" + userName + "\" "
						+ "AND nfProfile.userName = \"" + frndUserName + "\""
						+ "RETURN r";
				if (!trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("This friend was already not followed", DbQueryExecResult.QUERY_ERROR_GENERIC);
				};
				
				queryStr = "MATCH (nProfile:profile)-[r:follows]->(nfProfile:profile) WHERE nProfile.userName = \"" + userName + "\" "
						+ "AND nfProfile.userName = \"" + frndUserName + "\""
						+ "DELETE r";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
			return new DbQueryStatus("Friend unfollowed successfully", DbQueryExecResult.QUERY_OK);
		} catch (Exception e500) {
			return new DbQueryStatus("Failed to unfollow friend", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		String queryStr;
		Map<String, List<String>> data = new HashMap<String, List<String>>();
		
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "MATCH (nProfile:profile) WHERE nProfile.userName = \"" + userName + "\" RETURN nProfile";
				if (!trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("This user doesn't exit", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				};
				
				queryStr = "MATCH (nProfile:profile)-[r:follows]->(nfProfile:profile) WHERE nProfile.userName = \"" + userName + "\" "
						+ "RETURN nfProfile";
				StatementResult friendsResult = trans.run(queryStr);
				while (friendsResult.hasNext()) {
					Record friend = friendsResult.next();
                    String friendName = friend.get("nfProfile").get("userName", "");
                    List<String> songs = new ArrayList<String>();
                    queryStr = "MATCH (nPlaylist:playlist)-[r:includes]->(nSong:song) WHERE nPlaylist.plName = \"" + friendName + "-favorites\" "
    						+ "RETURN nSong";
                    StatementResult songsResult = trans.run(queryStr);
                    while (songsResult.hasNext()) {
                    	Record song = songsResult.next();
                    	OkHttpClient client = new OkHttpClient();
                    	String songId = song.get("nSong").get("songId", "");
                		Request request = new Request.Builder().url("http://localhost:3001/getSongTitleById/" + songId).build();
                		Call call = client.newCall(request);
                		Response response = call.execute();
                		String songName = new JSONObject(response.body().string()).getString("data");
                    	songs.add(songName);
                    }
					data.put(friendName, songs);
				}
                
				trans.success();
			}
			session.close();
			DbQueryStatus dbQueryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
			dbQueryStatus.setData(data);
			return dbQueryStatus;
		} catch (Exception e500) {
			return new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
}
