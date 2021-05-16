package com.csc301.profilemicroservice;

public interface PlaylistDriver {
	/**
	 * Adds a song node with only songId property
	 * @param songId only field to be included in the song
	 * @return DbQueryStatus including status and message
	 */
	DbQueryStatus addSong(String songId);
	
	/**
	 * Creates an includes relationship from userName-favorites playlist to song with songId
	 * @param userName liker
	 * @param songId id of the song liked
	 * @return DbQueryStatus including status and message
	 */
	DbQueryStatus likeSong(String userName, String songId);
	
	/**
	 * Removes the includes relationship from userName-favorites playlist to song with songId if it exists
	 * @param userName liker
	 * @param songId id of the song liked
	 * @return DbQueryStatus including status and message
	 */
	DbQueryStatus unlikeSong(String userName, String songId);
	
	/**
	 * Removes the song node with songId and all includes relationship to the song node
	 * @param songId id of the song to delete
	 * @return DbQueryStatus including status and message
	 */
	DbQueryStatus deleteSongFromDb(String songId);
}