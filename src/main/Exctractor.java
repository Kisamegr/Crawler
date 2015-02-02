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
	private HashMap<Long, Float> levenDistances;

	private String theSource;

	public Exctractor() {
		levenDistances = new HashMap<>();

		try {

			// ExtractGeneralUserInfo();
			ExtractInspectedUserInfo();
			// FindCopycats();

			for (Entry<Long, InspectedUser> user : userMap.entrySet()) {
				System.out.println(user.toString());
			}

		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			if (e.isCausedByNetworkIssue()) {
				Console.Log("Error looking up a User @Extractor");
				Console.WriteExceptionDump(e, e.getErrorCode());

			}
			e.printStackTrace();
		}

		for (Entry<Long, Float> e : levenDistances.entrySet()) {
			System.out.println(e.getKey() + "  -  " + e.getValue());
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

		DBCursor cursor = mongo.getFollowedStatusesCursor();

		System.out.println(cursor.size());

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

			if (!status.isRetweet())
				user.addTweets();
			else
				user.addRetweets();

			if (status.getInReplyToUserId() != -1)
				user.addReplies();

			user.addUserMentions(status.getUserMentionEntities().length);

			user.addRetweeted(status.getRetweetCount());

			user.addHashtags(status.getHashtagEntities().length);

			if (status.getHashtagEntities().length > 0)
				user.addHashtaggedTweets();

			if (status.getURLEntities().length > 0)
				user.addUrlTweets();

			String source = status.getSource();

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

	}

	public void FindCopycats() throws TwitterException {
		List<Number> userList = mongo.getInspectedUserIDs();

		System.out.println(userList);

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
				levenDistances.put(userID.longValue(), (float) totalSimilar / twitch.size());
			else
				levenDistances.put(userID.longValue(), (float) 0);

		}

	}

	public static void main(String[] args) {
		new Exctractor();

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
