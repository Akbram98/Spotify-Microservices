package com.eecs3311.songmicroservice;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping
public class SongController {

	@Autowired
	private final SongDal songDal;

	private OkHttpClient client = new OkHttpClient();

	
	public SongController(SongDal songDal) {
		this.songDal = songDal;
	}

	/**
	 * This method is partially implemented for you to follow as an example of
	 * how to complete the implementations of methods in the controller classes.
	 * @param songId
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
	public ResponseEntity<Map<String, Object>> getSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));

		DbQueryStatus dbQueryStatus = songDal.findSongById(songId);

		// TODO: uncomment these two lines when you have completed the implementation of findSongById in SongDal
		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
	}

	
	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
	public ResponseEntity<Map<String, Object>> getSongTitleById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in getSongById
		
		DbQueryStatus dbQueryStatus = songDal.getSongTitleById(songId);

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
	}

	
	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
	public ResponseEntity<Map<String, Object>> deleteSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("DELETE %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = songDal.deleteSongById(songId);

		if(dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK)
			sendDeleteSongRequest(songId);

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}

	
	
	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public ResponseEntity<Map<String, Object>> addSong(@RequestBody Map<String, String> params,
			HttpServletRequest request) {

		
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in getSongById
		String songName = params.get("songName");
		String songArtistFullName = params.get("songArtistFullName");
		String songAlbum = params.get("songAlbum");

		Song song = new Song(songName, songArtistFullName, songAlbum);
		song.setId(new ObjectId());

		DbQueryStatus dbQueryStatus = songDal.addSong(song);

		if(dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK)
			sendAddSongRequest(song.getId());
		

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
	}

	
	@RequestMapping(value = "/updateSongFavouritesCount", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> updateFavouritesCount(@RequestBody Map<String, String> params, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("data", String.format("PUT %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in getSongById
		String songId = params.get("songId");
		boolean shouldDecrement = false;

		if(params.get("shouldDecrement").equals("true"))
			shouldDecrement = true;

		DbQueryStatus dbQueryStatus = songDal.updateSongFavouritesCount(songId, shouldDecrement);

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}
	
	private void sendDeleteSongRequest(String songId){
		MediaType JSON = MediaType.parse("application/json; charset=utf-8");
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("songId", songId);
		
		okhttp3.RequestBody formBody = okhttp3.RequestBody.create(jsonObj.toString(), JSON);

		Request request = new Request.Builder()
				.url("http://localhost:3002/deleteSong")
				.delete(formBody)
				.build();

		try {
			Call call = client.newCall(request);
			call.execute().body().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	private void sendAddSongRequest(String songId){
		
		MediaType JSON = MediaType.parse("application/json; charset=utf-8");
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("songId", songId);
		
		okhttp3.RequestBody formBody = okhttp3.RequestBody.create(jsonObj.toString(), JSON);

		Request request = new Request.Builder()
				.url("http://localhost:3002/addSong")
				.post(formBody)
				.build();

		try {
			Call call = client.newCall(request);
			call.execute().body().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}