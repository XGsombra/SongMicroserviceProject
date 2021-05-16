package com.csc301.songmicroservice;

public interface SongDal {
	/**
	 * Adds a song to the mongoDB database
	 * @param songToAdd the song to be added
	 * @return DbQueryStatus including status, message and data: the song
	 */
	DbQueryStatus addSong(Song songToAdd);
	
	/**
	 * Retrieves the song given songId from MongoDB database
	 * @param songId id of the song to retrieve
	 * @return DbQueryStatus including status, message and data: the song
	 */
	DbQueryStatus findSongById(String songId);
	
	/**
	 * Retrieves the song title given songId from MongoDB database
	 * @param songId id of the song to retrieve title
	 * @return DbQueryStatus including status, message and data: song title
	 */
	DbQueryStatus getSongTitleById(String songId);
	
	/**
	 * Deletes the song with songId from MongoDB database
	 * @param songId id of the song to delete
	 * @return DbQueryStatus including status and message
	 */
	DbQueryStatus deleteSongById(String songId);	
	
	/**
	 * Increment or decrement songAmountFavourites of the song with songId
	 * @param songId id of the song to modify
	 * @param shouldDecrement if we should decrement the count, otherwise increment
	 * @return DbQueryStatus including status and message
	 */
	DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement);
}
