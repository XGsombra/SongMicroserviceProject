package com.csc301.songmicroservice;

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
public class SongController {

	@Autowired
	private final SongDal songDal;

	private OkHttpClient client = new OkHttpClient();

	
	public SongController(SongDal songDal) {
		this.songDal = songDal;
	}
	
	/**
	 * Retrieves the song given the songId
	 * Example call: GET /getSongById/5fc99a5dc4d9c055ac1bc0c9
	 * @param songId id of the song to retrieve
	 * @param request used to track path
	 * @return response body in map form, including status and data: song info 
	 */
	@RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));

		DbQueryStatus dbQueryStatus = songDal.findSongById(songId);

		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}
	
	/**
	 * Retrieves the song title given the songId
	 * Example call: GET /getSongTitleById/5fc99a5dc4d9c055ac1bc0c9
	 * @param songId id of the song to retrieve
	 * @param request used to track path
	 * @return response body in map form, including status and data: song title
	 */
	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongTitleById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = songDal.getSongTitleById(songId);

		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}
	
	/**
	 * Deletes the song from the MongoDB database and from all Profiles that have the songId added in their “favourites” list
	 * Example call: DELETE /deleteSongById/5fc99a5dc4d9c055ac1bc0c9
	 * @param songId id of the song to delete
	 * @param request used to track path
	 * @return response body in map form, including status
	 */
	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
	public @ResponseBody Map<String, Object> deleteSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("DELETE %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = songDal.deleteSongById(songId);
		
		if(dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
			Request requestToProfile = new Request.Builder().url("http://localhost:3002/deleteAllSongsFromDb/" + songId).put(Utils.emptyRequestBody).build();
			Call call = client.newCall(requestToProfile);
			try {
				call.execute();
			} catch (Exception e500) {
				return Utils.setResponseStatus(new HashMap<String, Object>(), DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			}
		}

		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}
	
	/**
	 * Adds a Song to the Song database both in mongoDB and neo4j 
	 * Example call: POST /addSong?songName=songName2&songArtistFullName=songArtistFullName2&songAlbum=songAlbum2
	 * @param params map including songName, songArtistFullName and songAlbum
	 * @param request used to track path
	 * @return response body in map form, including status and data: song info
	 */
	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addSong(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));

		String songName = params.get("songName");
		String songArtistFullName = params.get("songArtistFullName");
		String songAlbum = params.get("songAlbum");
		Song song = new Song(songName, songArtistFullName, songAlbum); 
		DbQueryStatus dbQueryStatus = songDal.addSong(song);
		
		if(dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
			Request requestToProfile = new Request.Builder().url("http://localhost:3002/addSong?songId=" + song.getId()).post(Utils.emptyRequestBody).build();
			Call call = client.newCall(requestToProfile);
			try {
				call.execute();
			} catch (Exception e500) {
				return Utils.setResponseStatus(new HashMap<String, Object>(), DbQueryExecResult.QUERY_ERROR_GENERIC, null);
			}
		}
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}
	
	/**
	 * Updates the song’s favourites count
	 * Example call: PUT updateSongFavouritesCount/5fc99a5dc4d9c055ac1bc0c9?shouldDecrement=true
	 * @param songId id of the song to update
	 * @param shouldDecrement if we should decrement the count, otherwise increment
	 * @param request used to track path
	 * @return response body in map form, including status
	 */
	@RequestMapping(value = "/updateSongFavouritesCount/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> updateFavouritesCount(@PathVariable("songId") String songId,
			@RequestParam("shouldDecrement") String shouldDecrement, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		boolean shouldDecrementBool;
		if(shouldDecrement.equals("true")) {
			shouldDecrementBool = true;
		} else if(shouldDecrement.equals("false")) {
			shouldDecrementBool = false;
		} else {
			return Utils.setResponseStatus(new HashMap<String, Object>(), DbQueryExecResult.QUERY_ERROR_GENERIC, null);
		}
		DbQueryStatus dbQueryStatus = songDal.updateSongFavouritesCount(songId, shouldDecrementBool);

		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());

		return response;
	}
}