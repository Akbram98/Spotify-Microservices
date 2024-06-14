package com.eecs3311.songmicroservice;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import com.mongodb.client.model.Filters;

import com.mongodb.MongoException;
import com.mongodb.client.model.UpdateOptions;


import static com.mongodb.client.model.Updates.*;

import java.util.List;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		DbQueryStatus qStatus;
		if(db.insert(songToAdd) != null) {
			qStatus = new DbQueryStatus("Song added successfully", DbQueryExecResult.QUERY_OK);
			qStatus.setData(songToAdd.getJsonRepresentation());
		}
		else
			qStatus = new DbQueryStatus("Failed to add song to database", DbQueryExecResult.QUERY_ERROR_GENERIC);

		return qStatus;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		DbQueryStatus qStatus;
		ObjectId oid = new ObjectId(songId);
		Song songFnd = null;
		
		List<Song> list = db.findAll(Song.class);
		
		if(list.size() > 0) {
			for(Song song: list) {
				
				if(oid.toHexString().equals(song.getId())) {
					songFnd = song;
					break;
				}
			
			}
		}
		else 
			qStatus = new DbQueryStatus("Database empty", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

		if(songFnd == null)
			qStatus = new DbQueryStatus("Song was not found!", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		else {
			qStatus = new DbQueryStatus("Song was found!", DbQueryExecResult.QUERY_OK);
		
			qStatus.setData(songFnd.getJsonRepresentation());
		}
			
		return qStatus;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		DbQueryStatus qStatus;
		ObjectId oid = new ObjectId(songId);
		Song song;

		if( (song = db.findById(oid.toHexString(), Song.class)) != null) {
			qStatus = new DbQueryStatus("Song was found successfully", DbQueryExecResult.QUERY_OK);
			qStatus.setData(song.getSongName());
		}
		else
			qStatus = new DbQueryStatus("Song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

		return qStatus;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		DbQueryStatus qStatus;
		ObjectId oid = new ObjectId(songId);
		
		Song song = db.findById(oid.toHexString(), Song.class);
		if(db.remove(song) != null)
			qStatus = new DbQueryStatus("Song removed successfully", DbQueryExecResult.QUERY_OK);
		else
			qStatus = new DbQueryStatus("Song doesn't exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

		return qStatus;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		DbQueryStatus qStatus;
		ObjectId oid = new ObjectId(songId);
		Song song;

		if( (song = db.findById(oid.toHexString(), Song.class)) != null) {
			long songFav = song.getSongAmountFavourites();
			
			if(shouldDecrement) songFav--;
			else 
				songFav++;
			
			if(songFav >= 0) {
				Document query = new Document().append("_id",  oid);
				Bson update =  set("songAmountFavourites", songFav);
				UpdateOptions options = new UpdateOptions().upsert(true);
			
				try {	
					db.getCollection("songs").updateOne(query, update, options);  
				} catch (MongoException me) {
					qStatus = new DbQueryStatus("Something went wrong with query", DbQueryExecResult.QUERY_ERROR_GENERIC);
					return qStatus;
				}
				
				qStatus = new DbQueryStatus("Song favourites was updated", DbQueryExecResult.QUERY_OK);
			}
			else
				qStatus = new DbQueryStatus("SongAmountFavourites not updated since it cannot be a negative value!", DbQueryExecResult.QUERY_OK);
			
		}
		else
			qStatus = new DbQueryStatus("Song doesn't exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);

		return qStatus;
	}
}