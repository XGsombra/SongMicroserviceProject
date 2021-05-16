package com.csc301.profilemicroservice;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.springframework.stereotype.Repository;

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
	
	@Override
	public DbQueryStatus addSong(String songId) {
		String queryStr;
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE (song:song{songId: \"" + songId + "\"})";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
			return new DbQueryStatus("Song added successfully", DbQueryExecResult.QUERY_OK);
		} catch (Exception e500) {
			return new DbQueryStatus("Song creation failed", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	@Override
	public DbQueryStatus likeSong(String userName, String songId) {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "MATCH (nPlaylist:playlist)-[r:includes]->(nSong:song) WHERE nPlaylist.plName = \"" + userName + "-favorites\" "
						+ "AND nSong.songId = \""+songId+"\""
						+ "return r";
				
				if (trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("Already liked", DbQueryExecResult.QUERY_OK);
				}
				
				queryStr = "MATCH (nPlaylist:playlist), (nSong:song) WHERE nPlaylist.plName = \"" + userName + "-favorites\" "
						+ "AND nSong.songId = \""+songId+"\"" 
						+ "CREATE (nPlaylist)-[r:includes]->(nSong)"
						+ "return r";
				
				if (!trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("Song or user doesn't exit", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				}

				trans.success();
			}
			session.close();
			return new DbQueryStatus("Song liked successfully", DbQueryExecResult.QUERY_OK);
		} catch (Exception e500) {
			return new DbQueryStatus("Failed to like song", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "MATCH (nPlaylist:playlist) WHERE nPlaylist.plName = \"" + userName + "-favorites\" RETURN nPlaylist";
				if (!trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("The playlist doesn't exit", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				}
				
				queryStr = "MATCH (nSong:song) WHERE nSong.songId = \"" + songId + "\" RETURN nSong";
				if (!trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("The playlist doesn't exit", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				}
				
				queryStr = "MATCH (nPlaylist:playlist)-[r:includes]->(nSong:song) WHERE nPlaylist.plName = \"" + userName + "-favorites\" "
						+ "AND nSong.songId = \""+songId+"\" RETURN r";
				if (!trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("The song was not liked", DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				
				
				queryStr = "MATCH (nPlaylist:playlist)-[r:includes]->(nSong:song) WHERE nPlaylist.plName = \"" + userName + "-favorites\" "
						+ "AND nSong.songId = \""+songId+"\" DELETE r";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
			return new DbQueryStatus("Friend followed successfully", DbQueryExecResult.QUERY_OK);
		} catch (Exception e500) {
			return new DbQueryStatus("Failed to follow friend", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "MATCH (nSong:song) WHERE nSong.songId = \"" + songId + "\" RETURN nSong";
				if (!trans.run(queryStr).hasNext()) {
					return new DbQueryStatus("Song or user doesn't exit", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				}
				
				queryStr = "MATCH (nPlaylist:playlist)-[r:includes]->(nSong:song) WHERE nSong.songId = \"" + songId + "\" DELETE r";
				trans.run(queryStr);
				
				queryStr = "MATCH (nSong:song) WHERE nSong.songId = \"" + songId + "\" DELETE nSong";
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
}
