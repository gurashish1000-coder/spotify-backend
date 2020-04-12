package com.csc301.songmicroservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class SongController {

	@Autowired
	private final SongDal songDal;

	private OkHttpClient client = new OkHttpClient();

	
	public SongController(SongDal songDal) {
		this.songDal = songDal;
	}
	
	// This is not one of the main methods.
	@RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		DbQueryStatus status = songDal.findSongById(songId);
		if (status.getData() == null) {
			response.put("status",status.getMessage());
			return response;
		} else {
			response.put("data",status.getData());
			return response;
		}
	}

	// This is the main method.
	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongTitleById(@PathVariable("songId") String songId,
			HttpServletRequest request) {
		Map<String, Object> response = new HashMap<String, Object>();
		DbQueryStatus status = songDal.getSongTitleById(songId);
		Song found_song = (Song) status.getData();
		if (status.getData() == null) {
			response.put("status",status.getMessage());
			return response;
		} else {
			response.put("status","OK");
			response.put("data",found_song.getSongName());
			return response;
		}
	}

	
	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
	public @ResponseBody Map<String, Object> deleteSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		DbQueryStatus status = songDal.deleteSongById(songId);
		//Song found_song = (Song) status.getData();
		if (status.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
			response.put("status","OK");
		} else if (status.getdbQueryExecResult() == DbQueryExecResult.QUERY_ERROR_NOT_FOUND) {
			response.put("status","ERROR NOT FOUND");
		} else {
			response.put("status","SongId invalid.");
		}
		return response;
	}
	
	/// Done till here.

	
	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addSong(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		Song newSong = new Song(params.get("songName"), params.get("songArtistFullName"), params.get("songAlbum"));
		
		if(params.get("songName") == null || params.get("songArtistFullName") == null || params.get("songAlbum") == null) {
			response.put("status", "Song not added: Song info fields missing.");
			return response;
		} else if (params.get("songName").equals(params.get("songArtistFullName"))) {
			response.put("status", "Song not Added. Song name and artist name not unique.");
			return response;
		}
		DbQueryStatus status = songDal.addSong(newSong);
		Song song = (Song) status.getData();
		response.put("data", song.getJsonRepresentation());
		response.put("status", status.getMessage());		
		return response;
	}

	
	@RequestMapping(value = "/updateSongFavouritesCount/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> updateFavouritesCount(@PathVariable("songId") String songId,
			@RequestParam("shouldDecrement") String shouldDecrement, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			if (shouldDecrement.equals("true")) {
				DbQueryStatus status = songDal.updateSongFavouritesCount(songId, true);
				response.put("status",status.getMessage());
				return response;
			} else if (shouldDecrement.equals("false")) {
				DbQueryStatus status = songDal.updateSongFavouritesCount(songId, false);
				response.put("status",status.getMessage());
				return response;
			} else {
				response.put("status", "Field value incorrect");
				return response;
			}
			
		} catch (Exception e) {
			response.put("status", "Something went wrong");
			return response;
		}
		
	}
}