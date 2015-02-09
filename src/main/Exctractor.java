package main;

import java.util.ArrayList;
import java.util.HashMap;
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

	HashMap<Long, InspectedUser> userMap = new HashMap<>();

	public Exctractor() {

		try {

			Console.Log("----- Started Extractor -----");

			Console.Log("Extracting General User Info...");
			// ExtractGeneralUserInfo();

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

	public void ExtractGeneralUserInfo() throws TwitterException {
		DBCursor cursor = mongo.getStatusesCursor(new BasicDBObject("trends", 0).append("index_id", 0));

		long currentTime = System.currentTimeMillis();

		for (int i = 0; i < cursor.size(); i++) {
			DBObject obj = cursor.next();
			Status status = TwitterObjectFactory.createStatus(obj.toString());

			User user = status.getUser();

			// System.out.println(obj.toString());
			long age = currentTime - user.getCreatedAt().getTime();
			int followers = user.getFollowersCount();
			int friends = user.getFriendsCount();
			mongo.addGeneralUserInfo(user.getId(), age, followers, friends);

		}

	}

	public void ExtractInspectedUserInfo() throws TwitterException {
		HashMap<Long, HashMap<String, Integer>> sourcesCounter = new HashMap<>();

		long currentTime = System.currentTimeMillis();

		DBCursor cursor = mongo.getFollowedStatusesCursor();

		for (int i = 0; i < cursor.count(); i++) {
			DBObject obj = cursor.next();
			Status status = TwitterObjectFactory.createStatus(obj.toString());

			long id = status.getUser().getId();
			InspectedUser user;

			if (!userMap.containsKey(id)) {

				user = new InspectedUser();
				user.setId(id);
				userMap.put(id, user);
			} else
				user = userMap.get(id);

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

			String fullSource = status.getSource();
			String source = fullSource.split("<|>")[2];

			if (!sourcesCounter.containsKey(user.getId()))
				sourcesCounter.put(user.getId(), new HashMap<String, Integer>());

			HashMap<String, Integer> userSourceMap = sourcesCounter.get(user.getId());

			if (userSourceMap.containsKey(source))
				userSourceMap.put(source, userSourceMap.get(source) + 1);
			else
				userSourceMap.put(source, 1);

		}

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

		// Delete users that have only a few tweets
		for (Entry<Long, InspectedUser> en : userMap.entrySet()) {
			if (en.getValue().getTweets() + en.getValue().getRetweets() < 5)
				userMap.remove(en.getKey());

		}

	}

	public void FindDuplicates() throws TwitterException {
		List<Number> userList = mongo.getInspectedUserIDs();

		for (Number userID : userList) {

			DBCursor cursor = mongo.getFollowedStatusesCursor(userID.longValue());
			ArrayList<String> twitch = new ArrayList<>();

			for (int i = 0; i < cursor.size(); i++) {
				DBObject obj = cursor.next();

				Status status = TwitterObjectFactory.createStatus(obj.toString());

				// System.out.println(status.getText());

				if (!status.isRetweet() && status.getInReplyToUserId() == -1) {

					StringBuilder tweet = new StringBuilder();

					tweet.append(status.getText());

					for (UserMentionEntity mention : status.getUserMentionEntities()) {
						int index_start = tweet.indexOf(mention.getText());

						if (index_start != -1)
							tweet.delete(index_start, index_start + mention.getText().length());
					}

					for (URLEntity url : status.getURLEntities()) {
						int index_start = tweet.indexOf(url.getText());

						if (index_start != -1)
							tweet.delete(index_start, index_start + url.getText().length());
					}

					twitch.add(tweet.toString());

				}
			}
			int totalSimilar = 0;

			for (int x = 0; x < twitch.size(); x++) {
				for (int y = x + 1; y < twitch.size(); y++) {
					float percent = LevenshteinDistance(twitch.get(x), twitch.get(y));
					// System.out.println(percent);
					if (percent < 0.1)
						totalSimilar++;

				}
			}

			if (twitch.size() > 0)
				userMap.get(userID.longValue()).setDuplicateRatio((double) totalSimilar / twitch.size());
			else
				userMap.get(userID.longValue()).setDuplicateRatio(0);

		}

	}

	public void SaveInspectedUsersMongo() {

		for (Entry<Long, InspectedUser> entry : userMap.entrySet()) {
			mongo.addInspectedUserInfo(entry.getValue());
		}
	}

	public static void main(String[] args) {
		new Exctractor();
		new Analyzer();

	}

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
