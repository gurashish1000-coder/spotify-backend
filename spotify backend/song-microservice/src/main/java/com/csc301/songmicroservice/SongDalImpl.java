package com.csc301.songmicroservice;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;
	
	// Okay so i clearly need to add code here for methods.
	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}
	
	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		Song song  = songToAdd;
		db.save(songToAdd);
		DbQueryStatus status = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		status.setData(songToAdd);
		return status;
	}


	@Override
	public DbQueryStatus findSongById(String songId) {
		// TODO Auto-generated method stub
		
		Song found_song = db.findById(songId, Song.class);
		// This case is supposed to check if a song with the given id exists in the database.
		if (found_song == null) {
			DbQueryStatus status = new DbQueryStatus("No song found with the id",DbQueryExecResult.QUERY_ERROR_GENERIC);
			status.setData(found_song);
			return status;
		}
		
		String result  = found_song.getSongName();
		DbQueryStatus status = new DbQueryStatus(result,DbQueryExecResult.QUERY_OK);
		status.setData(found_song);
		return status;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		// TODO Auto-generated method stub
		Song found_song = db.findById(songId, Song.class);
		
		// This case is supposed to check if a song with the given id exists in the database.
		if (found_song == null) {
			DbQueryStatus status = new DbQueryStatus("No song found with the id", DbQueryExecResult.QUERY_ERROR_GENERIC);
			status.setData(found_song);
			return status;
		}
		
		String result  = found_song.getSongName();
		DbQueryStatus status = new DbQueryStatus(result,DbQueryExecResult.QUERY_OK);
		status.setData(found_song);
		return status;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		// TODO Auto-generated method stub
		try {
			Song found_song = db.findById(songId, Song.class);
			// This case is supposed to check if a song with the given id exists in the database.
			if (found_song == null) {
				DbQueryStatus status = new DbQueryStatus("No song found with the id", DbQueryExecResult.QUERY_ERROR_GENERIC);
				status.setData(found_song);
				return status;
			} else {
				String result  = found_song.getSongName();
				DbQueryStatus status = new DbQueryStatus(result, DbQueryExecResult.QUERY_OK);
				status.setData(found_song);
				db.remove(found_song);
				return status;
			}
		} catch (Exception e) {
			String result = "Something went wrong";
			DbQueryStatus status = new DbQueryStatus(result, DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}
	}
	// Just this method left to be worked on.
	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
		try {
			Song found_song = db.findById(songId, Song.class);
			if (found_song == null) {
				DbQueryStatus status = new DbQueryStatus("No song found with the id", DbQueryExecResult.QUERY_ERROR_GENERIC);
				status.setData(found_song);
				return status;
			} else {
				db.remove(found_song);
				if(shouldDecrement) {
					// If true
					found_song.setSongAmountFavourites(found_song.getSongAmountFavourites() + 1);
					
				} else {
					// If false
					found_song.setSongAmountFavourites(found_song.getSongAmountFavourites() - 1);
				}
				db.save(found_song);
				String result = "OK";
				DbQueryStatus status = new DbQueryStatus(result, DbQueryExecResult.QUERY_OK);
				return status;
			}
		} catch (Exception e) {
			String result = "Something went wrong";
			DbQueryStatus status = new DbQueryStatus(result, DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			return status;
		}
	}
}