package com.csc301.profilemicroservice;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@RestController
@RequestMapping("/")
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}
	
	/**
	 * Adds a profile and its playlist to the Profile database
	 * Example call: POST /profile?userName=userName3&fullName=fullName3&password=password3
	 * @param params map including userName, fullName and password
	 * @param request used to track path
	 * @return response body in map form, including status
	 */
	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addProfile(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		String userName;
		String fullName;
		String password;
		userName = params.get("userName");
		fullName = params.get("fullName");
		password = params.get("password");
		DbQueryStatus dbQueryStatus = profileDriver.createUserProfile(userName, fullName, password);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}
	
	/**
	 * Allows a Profile to follow another Profile and become a friend
	 * body in map form, including status
	 * @param userName follower
	 * @param friendUserName followed
	 * @param request used to track path
	 * @return body in map form, including status
	 */
	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = profileDriver.followFriend(userName, friendUserName);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		response.put("message", dbQueryStatus.getMessage());
	    return response;
	}
	
	/**
	 * Returns the Song names of all of the Songs that the User’s friends have liked
	 * Example call: GET /getAllFriendFavouriteSongTitles/userName1
	 * @param userName user name of the profile to get friends liked songs
	 * @param request used to track path
	 * @return body in map form, including status and data: list of friends with their like song titles
	 */
	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = profileDriver.getAllSongFriendsLike(userName);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
	    return response;
	}

	/**
	 * Allows a Profile to unfollow another Profile and no longer be "friends" with them
	 * Example call: PUT /unfollowFriend/userName1/userName2
	 * @param userName follower
	 * @param friendUserName followed
	 * @param request used to track path
	 * @return body in map form, including status
	 */
	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = profileDriver.unfollowFriend(userName, friendUserName);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
	    return response;
	}
	
	/**
	 * Allows a Profile to like a song and add it to their favourites. You can like the same song twice.
	 * Example call: PUT /likeSong/userName1/5fc99a5dc4d9c055ac1bc0c9
	 * @param userName liker
	 * @param songId id of liked song
	 * @param request used to track path
	 * @return body in map form, including status
	 */
	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = playlistDriver.likeSong(userName, songId);
		
		if(dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK && !dbQueryStatus.getMessage().equals("Already liked")) {
			Request requestToProfile = new Request.Builder().url("http://localhost:3001/updateSongFavouritesCount/" + songId + "?shouldDecrement=false").put(Utils.emptyRequestBody).build();
			Call call = client.newCall(requestToProfile);
			try {
				call.execute();
			} catch (Exception e500) {
				return Utils.setResponseStatus(new HashMap<String, Object>(), DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			}
		}
		
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		response.put("message", dbQueryStatus.getMessage());
	    return response;
	}
	
	/**
	 * Allows a Profile to unlike a song and remove it from their favourites. You cannot unlike the same song twice
	 * Example call: PUT /unlikeSong/userName1/5fc99a5dc4d9c055ac1bc0c9
	 * @param userName liker
	 * @param songId id of liked song
	 * @param request used to track path
	 * @return body in map form, including status
	 */
	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = playlistDriver.unlikeSong(userName, songId);
		
		if(dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
			Request requestToProfile = new Request.Builder().url("http://localhost:3001/updateSongFavouritesCount/" + songId + "?shouldDecrement=true").put(Utils.emptyRequestBody).build();
			Call call = client.newCall(requestToProfile);
			try {
				call.execute();
			} catch (Exception e500) {
				return Utils.setResponseStatus(new HashMap<String, Object>(), DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			}
		}
		
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
	    return response;
	}
	
	/**
	 * Deletes existence the song with songId from profile database
	 * Example call: PUT /deleteAllSongsFromDb/5fc99a5dc4d9c055ac1bc0c9
	 * @param songId id of the song to delete
	 * @param request used to track path
	 * @return in map form, including status
	 */
	@RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> deleteAllSongsFromDb(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = playlistDriver.deleteSongFromDb(songId);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
	    return response;
	}
	
	/**
	 * Adds a song to profile database with only an songId
	 * Example call: POST /addSong?songId=5fc99a5dc4d9c055ac1bc0c9
	 * @param songId only field to be included in the song
	 * @param request used to track path
	 * @return in map form, including status
	 */
	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addSong(@RequestParam("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = playlistDriver.addSong(songId);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
	    return response;
	}
}