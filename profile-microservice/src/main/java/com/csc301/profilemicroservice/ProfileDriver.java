package com.csc301.profilemicroservice;

public interface ProfileDriver {
	/**
	 * Adds a profile node and its playlist userName-favorites
	 * @param userName user name of the profile
	 * @param fullName full name of the profile
	 * @param password password of the profile
	 * @return DbQueryStatus including status and message
	 */
	DbQueryStatus createUserProfile(String userName, String fullName, String password);
	
	/**
	 * Creates a follows relationship from profile with userName to profile with frndUserName
	 * @param userName follower
	 * @param frndUserName followed
	 * @return DbQueryStatus including status and message
	 */
	DbQueryStatus followFriend(String userName, String frndUserName);
	
	/**
	 * Removes the follows relationship from profile with userName to profile with frndUserName if it exists
	 * @param userName follower
	 * @param frndUserName followed
	 * @return DbQueryStatus including status and message
	 */
	DbQueryStatus unfollowFriend(String userName, String frndUserName );
	
	/**
	 * Retrieves the names of songs liked by friends of the profile with userName
	 * @param userName user name of the profile to get friends liked songs
	 * @return DbQueryStatus including status, message and data: list of friends with their like song titles
	 */
	DbQueryStatus getAllSongFriendsLike(String userName);
}