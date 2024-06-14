package com.eecs3311.profilemicroservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eecs3311.profilemicroservice.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";
	public static final String KEY_FRIEND_USER_NAME = "friendUserName";
	public static final String KEY_SONG_ID = "songId";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}

	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public ResponseEntity<Map<String, Object>> addProfile(@RequestBody Map<String, String> params, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));

		String userName = params.get("userName");
		String fullName = params.get("fullName");
		String password = params.get("password");

		DbQueryStatus dbQueryStatus = profileDriver.createUserProfile(userName,fullName, password);

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}

	@RequestMapping(value = "/followFriend", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> followFriend(@RequestBody Map<String, String> params, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in SongController.getSongById
		String userName = params.get("userName");
		String frndUserName = params.get("friendUserName");

		DbQueryStatus dbQueryStatus = profileDriver.followFriend(userName, frndUserName);

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}

	@RequestMapping(value = "/changeAccess", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> changeAccess(@RequestBody Map<String, String> params, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in SongController.getSongById
		String plName = params.get("plName");

		DbQueryStatus dbQueryStatus = playlistDriver.changePlaylistAccess(plName);

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}

	@RequestMapping(value = "/subscribe", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> subscribe(@RequestBody Map<String, String> params, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in SongController.getSongById
		String plName = params.get("plName");
		String userName = params.get("userName");

		DbQueryStatus dbQueryStatus = playlistDriver.subscribeToPlaylist(userName, plName);

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}
	
	@RequestMapping(value = "/unsubscribe", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> unsubscribe(@RequestBody Map<String, String> params, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in SongController.getSongById
		String plName = params.get("plName");
		String userName = params.get("userName");

		DbQueryStatus dbQueryStatus = playlistDriver.unSubscribeToPlaylist(userName, plName);

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}

	@RequestMapping(value = "/deleteSong", method = RequestMethod.DELETE)
	public ResponseEntity<Map<String, Object>> deleteSong(@RequestBody Map<String, String> params,
																HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in getSongById
		String songId = params.get("songId");
		DbQueryStatus dbQueryStatus = profileDriver.deleteSongById(songId);

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}
	
	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public ResponseEntity<Map<String, Object>> addSong(@RequestBody Map<String, String> params, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));

		String songId = params.get("songId");

		DbQueryStatus dbQueryStatus = profileDriver.addSongId(songId);

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}

	@RequestMapping(value = "/getPublicPlaylists/{userName}", method = RequestMethod.GET)
	public ResponseEntity<Map<String, Object>> getPublicPlaylists(@PathVariable("userName") String userName,
																			   HttpServletRequest request) {
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in SongController.getSongById
		DbQueryStatus dbQueryStatus = playlistDriver.getPlaylists(userName);

		if(dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK){
			HashMap<String, List<String>> public_playlists = new HashMap<String, List<String>>();
			HashMap<String, List<String>> data = (HashMap<String, List<String>>)playlistDriver.getPlaylistSongIdList();

			for(Entry<String, List<String>> entry: data.entrySet()){
				List<String> songData = new ArrayList<String>();
				for(String songId: entry.getValue()){
			
					JSONObject resp_data = sendGetsongTitleRequest(songId);
					songData.add(resp_data.getString("data"));
				}
				
				public_playlists.put(entry.getKey(), songData);
			}

			
			dbQueryStatus.setData(public_playlists);
			dbQueryStatus.setMessage("Public Playlists for user: " + userName + " fetched");

		}
		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
	}



	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public ResponseEntity<Map<String, Object>> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in SongController.getSongById
		DbQueryStatus dbQueryStatus = profileDriver.getAllSongFriendsLike(userName);

		if(dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK){
			HashMap<String, List<String>> frndSongNames = new HashMap<String, List<String>>();
			HashMap<String, List<String>> data = (HashMap<String, List<String>>)profileDriver.getFriendSongIdList();

			for(Entry<String, List<String>> entry: data.entrySet()){
				List<String> songNames = new ArrayList<String>();
				for(String songId: entry.getValue()){
					JSONObject resp_data = sendGetsongTitleRequest(songId); 
					songNames.add(resp_data.getString("data"));
				}
				
				frndSongNames.put(entry.getKey(), songNames);
			}

			dbQueryStatus.setData(frndSongNames);
			dbQueryStatus.setMessage("Song titles were fetched");

		}
		
		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
	}

	private JSONObject sendGetsongTitleRequest(String songId){
		
		Request request = new Request.Builder()
				.url("http://localhost:3001/getSongTitleById/"+songId)
				.get()
				.build();

		try {
			Call call = client.newCall(request);
			Response response = call.execute();
			JSONObject json = new JSONObject(response.body().string());
			response.body().close();
			return json;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	private void sendUpdateFovoritesCntRequest(String songId, String shouldDecrement){
		
		MediaType JSON = MediaType.parse("application/json; charset=utf-8");
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("songId", songId);
		jsonObj.put("shouldDecrement", shouldDecrement);
		
		okhttp3.RequestBody formBody = okhttp3.RequestBody.create(jsonObj.toString(), JSON);

		Request request = new Request.Builder()
				.url("http://localhost:3001/updateSongFavouritesCount")
				.put(formBody)
				.build();

		try {
			Call call = client.newCall(request);
			call.execute().body().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/unfollowFriend", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> unfollowFriend(@RequestBody Map<String, String> params, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in SongController.getSongById
		String userName = params.get("userName");
		String frndUserName = params.get("friendUserName");

		DbQueryStatus dbQueryStatus = profileDriver.unfollowFriend(userName, frndUserName);

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}

	@RequestMapping(value = "/likeSong", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> likeSong(@RequestBody Map<String, String> params, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in SongController.getSongById
		String userName = params.get("userName");
		String songId = params.get("songId");

		DbQueryStatus dbQueryStatus = playlistDriver.likeSong(userName, songId);

		if(dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK)
			sendUpdateFovoritesCntRequest(songId, "false");

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}

	@RequestMapping(value = "/unlikeSong", method = RequestMethod.PUT)
	public ResponseEntity<Map<String, Object>> unlikeSong(@RequestBody Map<String, String> params, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		// TODO: add any other values to the map following the example in SongController.getSongById
		String userName = params.get("userName");
		String songId = params.get("songId");

		DbQueryStatus dbQueryStatus = playlistDriver.unlikeSong(userName, songId);

		if(dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK)
			sendUpdateFovoritesCntRequest(songId, "true");

		response.put("message", dbQueryStatus.getMessage());
		return Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), null);
	}
}