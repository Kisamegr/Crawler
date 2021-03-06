package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;
import util.InspectedUser;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class Exctractor extends StreamingAPI {

	// This hashmap holds the InspectedUser objects for each inspected user chosen in the Inspector class
	HashMap<Long, InspectedUser> userMap = new HashMap<>();

	public Exctractor() {

		try {

			Console.Log("----- Started Extractor -----");

			/* Extract data from the statuses */

			Console.Log("Extracting General User Info...");
			ExtractGeneralUserInfo();

			Console.Log("Extracting Inspected User Info...");
			ExtractInspectedUserInfo();

			Console.Log("Finding Inspected Users Duplicates...");
			FindDuplicates();

			SaveInspectedUsersMongo();

		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			Console.Log("Error @Extractor");
			if (e.isCausedByNetworkIssue()) {
				Console.WriteExceptionDump(e, e.getErrorCode());

			}
		}

	}

	// Extract the basic stats for each user
	public void ExtractGeneralUserInfo() throws TwitterException {
		DBCursor cursor = mongo.getStatusesCursor(new BasicDBObject("trends", 0).append("index_id", 0));

		long currentTime = System.currentTimeMillis();

		// For each status saved from the Receiver Class...
		for (int i = 0; i < cursor.size(); i++) {

			DBObject obj = cursor.next();

			// Make a status object from the JSON
			Status status = TwitterObjectFactory.createStatus(obj.toString());

			// Get the user from the status object
			User user = status.getUser();

			// Get the stats
			long age = currentTime - user.getCreatedAt().getTime();
			int followers = user.getFollowersCount();
			int friends = user.getFriendsCount();

			// Save them to the Mongo Database
			mongo.addGeneralUserInfo(user.getId(), age, followers, friends);

		}

	}

	// Extract the stats for each inspected user
	public void ExtractInspectedUserInfo() throws TwitterException {
		HashMap<Long, HashMap<String, Integer>> sourcesCounter = new HashMap<>();

		long currentTime = System.currentTimeMillis();

		DBCursor cursor = mongo.getFollowedStatusesCursor();

		// For each status saved from the Inspector Class...
		for (int i = 0; i < cursor.count(); i++) {
			DBObject obj = cursor.next();

			// Make a status object from the JSON
			Status status = TwitterObjectFactory.createStatus(obj.toString());

			// Get the user
			long id = status.getUser().getId();

			InspectedUser user;

			// If the user does not exist in the user map, then add him.
			// If the user exists, then get his InspectedUser Object
			if (!userMap.containsKey(id)) {

				user = new InspectedUser();
				user.setId(id);
				userMap.put(id, user);
			} else
				user = userMap.get(id);

			/* Add all his stats into the his InspectedUser Object */

			user.setFollowers(status.getUser().getFollowersCount());
			user.setFriends(status.getUser().getFriendsCount());
			user.setAge(currentTime - status.getUser().getCreatedAt().getTime());

			if (!status.isRetweet())
				user.addTweets();
			else
				user.addRetweets();

			if (status.getInReplyToUserId() != -1)
				user.addReplies();

			user.addUserMentions(status.getUserMentionEntities().length);

			user.addHashtags(status.getHashtagEntities().length);

			if (status.getHashtagEntities().length > 0)
				user.addHashtaggedTweets();

			if (status.getURLEntities().length > 0)
				user.addUrlTweets();

			// Get his source
			String fullSource = status.getSource();
			String source = fullSource.split("<|>")[2];

			// Add the user's source map to the general source map
			if (!sourcesCounter.containsKey(user.getId()))
				sourcesCounter.put(user.getId(), new HashMap<String, Integer>());

			HashMap<String, Integer> userSourceMap = sourcesCounter.get(user.getId());

			// Add 1 to the user's source map, in order to find his max source later
			if (userSourceMap.containsKey(source))
				userSourceMap.put(source, userSourceMap.get(source) + 1);
			else
				userSourceMap.put(source, 1);

		}

		// Count the frequency of each source for each user and add his max source
		for (Entry<Long, HashMap<String, Integer>> userSources : sourcesCounter.entrySet()) {

			String maxSource = null;
			int max = -1;

			for (Entry<String, Integer> s : userSources.getValue().entrySet()) {
				if (s.getValue() > max) {
					maxSource = s.getKey();
					max = s.getValue();
				}
			}

			userMap.get(userSources.getKey()).setMaxSource(maxSource);

		}

		// Delete users that have only a few tweets / Reduce Noise
		Iterator<Entry<Long, InspectedUser>> it = userMap.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Long, InspectedUser> en = it.next();
			if (en.getValue().getTweets() + en.getValue().getRetweets() < 5)
				it.remove();

		}

	}

	// Find the duplicates for each inspected user
	public void FindDuplicates() throws TwitterException {
		List<Number> userList = mongo.getInspectedUserIDs();

		// For each inspected user id...
		for (Number userID : userList) {

			// Get the InspectedUser Object from the map
			InspectedUser user = userMap.get(userID.longValue());

			// If the object exists... (means the user has not been cut as noise)
			if (user != null) {

				DBCursor cursor = mongo.getFollowedStatusesCursor(userID.longValue());

				// Hold the processed statuses here
				ArrayList<String> twitch = new ArrayList<>();

				// For each status of this user...
				for (int i = 0; i < cursor.size(); i++) {
					DBObject obj = cursor.next();

					// Create a status object from the JSON
					Status status = TwitterObjectFactory.createStatus(obj.toString());

					// If the status is not a retweet or reply, meaning it is a normal tweet
					if (!status.isRetweet() && status.getInReplyToUserId() == -1) {

						StringBuilder tweet = new StringBuilder();

						tweet.append(status.getText());

						// Remove the user mentions
						for (UserMentionEntity mention : status.getUserMentionEntities()) {
							int index_start = tweet.indexOf(mention.getText());

							if (index_start != -1)
								tweet.delete(index_start, index_start + mention.getText().length());
						}

						// Remove the urls
						for (URLEntity url : status.getURLEntities()) {
							int index_start = tweet.indexOf(url.getText());

							if (index_start != -1)
								tweet.delete(index_start, index_start + url.getText().length());
						}

						// Hold the processed status
						twitch.add(tweet.toString());

					}
				}
				int totalSimilar = 0;

				// Find the Lev Distances between each status and count the similars
				for (int x = 0; x < twitch.size(); x++) {
					for (int y = x + 1; y < twitch.size(); y++) {
						float percent = LevenshteinDistance(twitch.get(x), twitch.get(y));
						// System.out.println(percent);
						if (percent < 0.1)
							totalSimilar++;

					}
				}

				// Add the duplicate statuses percent into the user's object
				if (twitch.size() > 0)
					user.setDuplicateRatio((double) totalSimilar / twitch.size());
				else
					user.setDuplicateRatio(0);
			}
		}

	}

	// Save the stats of the inspected users into the Mongo Database
	public void SaveInspectedUsersMongo() {

		for (Entry<Long, InspectedUser> entry : userMap.entrySet()) {
			mongo.addInspectedUserInfo(entry.getValue());
		}
	}

	public static void main(String[] args) {
		new Exctractor();
		new Analyzer();

	}

	// NOT IMPLEMENTED BY US - FOUND ON THE INTERNET
	// Calculate the distance
	public float LevenshteinDistance(String s0, String s1) {
		int len0 = s0.length() + 1;
		int len1 = s1.length() + 1;

		// the array of distances
		int[] cost = new int[len0];
		int[] newcost = new int[len0];

		// initial cost of skipping prefix in String s0
		for (int i = 0; i < len0; i++)
			cost[i] = i;

		// dynamically computing the array of distances

		// transformation cost for each letter in s1
		for (int j = 1; j < len1; j++) {
			// initial cost of skipping prefix in String s1
			newcost[0] = j;

			// transformation cost for each letter in s0
			for (int i = 1; i < len0; i++) {
				// matching current letters in both strings
				int match = (s0.charAt(i - 1) == s1.charAt(j - 1)) ? 0 : 1;

				// computing cost for each transformation
				int cost_replace = cost[i - 1] + match;
				int cost_insert = cost[i] + 1;
				int cost_delete = newcost[i - 1] + 1;

				// keep minimum cost
				newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
			}

			// swap cost/newcost arrays
			int[] swap = cost;
			cost = newcost;
			newcost = swap;
		}

		// the distance is the cost for transforming all letters in both strings
		return (float) cost[len0 - 1] / (s0.length() + s1.length());
	}
}
