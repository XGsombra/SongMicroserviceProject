package com.csc301.songmicroservice;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}
	
	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		try {
			if(songToAdd.getSongAlbum() == null || 
					songToAdd.getSongAlbum().isEmpty() || 
					songToAdd.getSongArtistFullName() == null ||
					songToAdd.getSongArtistFullName().isEmpty() ||
					songToAdd.getSongName() == null ||
					songToAdd.getSongName().isEmpty()) {
				return new DbQueryStatus("Parameter incorrect", DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
			Song song = this.db.insert(songToAdd);
			DbQueryStatus dbQueryStatus = new DbQueryStatus("Song added successfully", DbQueryExecResult.QUERY_OK);
			dbQueryStatus.setData(song.getJsonRepresentation());
			return dbQueryStatus;
		} catch(Exception e500) {
			return new DbQueryStatus("Error occured", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	@Override
	public DbQueryStatus findSongById(String songId) {
		try {
			if(!ObjectId.isValid(songId)) {
				return new DbQueryStatus("No song with this ID exists", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			Song song = this.db.findOne(new Query(Criteria.where("_id").is(new ObjectId(songId))), Song.class);
			if (song == null) {
				return new DbQueryStatus("No song with this ID exists", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			DbQueryStatus dbQueryStatus = new DbQueryStatus("Song returned successfully", DbQueryExecResult.QUERY_OK);
			dbQueryStatus.setData(song.getJsonRepresentation());
			return dbQueryStatus;
		}catch(Exception e500) {
			return new DbQueryStatus("Failed return song", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		try {
			if(!ObjectId.isValid(songId)) {
				return new DbQueryStatus("No song with this ID exists", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			Song song = this.db.findOne(new Query(Criteria.where("_id").is(new ObjectId(songId))), Song.class);
			if (song == null) {
				return new DbQueryStatus("No song with this ID exists", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			DbQueryStatus dbQueryStatus = new DbQueryStatus("Song title returned successfully", DbQueryExecResult.QUERY_OK);
			dbQueryStatus.setData(song.getSongName());
			return dbQueryStatus;
		}catch(Exception e500) {
			return new DbQueryStatus("Failed return song title", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	@Override
	public DbQueryStatus deleteSongById(String songId) {
		try {
			if(!ObjectId.isValid(songId)) {
				return new DbQueryStatus("No song with this ID exists", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			Song song = this.db.findOne(new Query(Criteria.where("_id").is(new ObjectId(songId))), Song.class);
			if (song == null) {
				return new DbQueryStatus("No song with this ID exists", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			this.db.findAllAndRemove(new Query(Criteria.where("_id").is(new ObjectId(songId))), Song.class);
			return new DbQueryStatus("Song deleted successfully", DbQueryExecResult.QUERY_OK);
		}catch(Exception e500) {
			return new DbQueryStatus("Failed to delete song", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
	
	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		try {
			if(!ObjectId.isValid(songId)) {
				return new DbQueryStatus("No song with this ID exists", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			Song song = this.db.findOne(new Query(Criteria.where("_id").is(new ObjectId(songId))), Song.class);
			if (song == null) {
				return new DbQueryStatus("No song with this ID exists", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			
			long count = song.getSongAmountFavourites();
			if (shouldDecrement) {
				song.setSongAmountFavourites(count-1);
			} else {
				song.setSongAmountFavourites(count+1);
			}
			this.db.save(song, "songs");
			return new DbQueryStatus("Song favorite count updated successfully", DbQueryExecResult.QUERY_OK);	
			
		}catch(Exception e500) {
			return new DbQueryStatus("Failed to update song favorite count", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
}