package main;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import twitter4j.Trend;
import twitter4j.Trends;
import util.InspectedUser;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.util.JSON;

public class MongoDB {

	MongoClient mongoClient;
	DB db;
	DBCollection trendsColl;
	DBCollection statusColl;
	DBCollection uniqueTrendsColl;
	DBCollection followedColl;
	DBCollection generalUserColl;
	DBCollection inspectedUserColl;

	ArrayList<Trend> oldTrends;

	public MongoDB() {

		try {

			mongoClient = new MongoClient("localhost");

			db = mongoClient.getDB("twitter");

			trendsColl = db.getCollection("trends");
			statusColl = db.getCollection("statuses");
			uniqueTrendsColl = db.getCollection("unique_trends");
			followedColl = db.getCollection("followed");
			generalUserColl = db.getCollection("general_user");
			inspectedUserColl = db.getCollection("inspected_user");

			statusColl.createIndex(new BasicDBObject("index_id", "text"));

			trendsColl.createIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));
			uniqueTrendsColl.createIndex(new BasicDBObject("user", 1).append("trend", 1), new BasicDBObject("unique", true));
			generalUserColl.createIndex(new BasicDBObject("user", 1), new BasicDBObject("unique", true));
			inspectedUserColl.createIndex(new BasicDBObject("user", 1), new BasicDBObject("unique", true));

		} catch (UnknownHostException | MongoException e) {
			Console.Log("Mongo Exception at Initializing");
			Console.Log(e.getMessage());

			Console.WriteExceptionDump(e, 0);
		}

		oldTrends = new ArrayList<>();

	}

	public void updateTrends(Trends trends, Date now) {

		// DB db = mongoClient.getDB("twitter");

		// DBCollection coll = db.getCollection("trends");

		Console.Log("| CURRENT TRENDS");
		for (Trend t : trends.getTrends()) {
			Console.Log("|  " + t.getName());

			try {
				trendsColl.insert(new BasicDBObject("name", t.getName()).append("arrival", now).append("withdrawal", "not-finished"));
			} catch (MongoException e) {
				if (e.getCode() != 11000) { // Check if it is duplicate
					// insertion exception
					Console.Log("Mongo Exception at Inserting Trends");
					Console.Log(e.getMessage());
				}
			}
		}

		Console.Log("| -=-=-=-=-=-=-=-=-");
		Console.Log("| OLD TRENDS");

		for (Trend t : oldTrends) {
			Console.Log("|  " + t.getName());
		}

		Console.Log("| -=-=-=-=-=-=-=-=-");

		Console.Log("| TRENDS CHANGED");

		if (!oldTrends.isEmpty()) {
			for (Trend old : oldTrends) {
				boolean found = false;

				try {
					for (Trend t : trends.getTrends()) {

						if (old.getName().compareTo(t.getName()) == 0) {

							found = true;
							break;
						}

					}

					if (!found) {
						BasicDBObject newDocument = new BasicDBObject();
						newDocument.append("$set", new BasicDBObject().append("withdrawal", now));

						BasicDBObject searchQuery = new BasicDBObject().append("name", old.getName());

						trendsColl.update(searchQuery, newDocument);

						Console.Log("|  Changing " + old.getName() + " to finished");
					}
				} catch (MongoException e) {
					Console.Log("Mongo Exception at Checking Changed Trends");
					Console.Log(e.getMessage());
				}
			}
		}

		oldTrends.clear();

		for (Trend t : trends.getTrends()) {
			oldTrends.add(t);
		}

	}

	public ArrayList<String> getActiveTrends() {

		ArrayList<String> trends = new ArrayList<>();

		try {
			DB db = mongoClient.getDB("twitter");

			DBCollection coll = db.getCollection("trends");

			BasicDBObject q1 = new BasicDBObject("withdrawal", "not-finished");
			BasicDBObject q2 = new BasicDBObject("withdrawal", new BasicDBObject("$gte", new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 2)));

			BasicDBList or = new BasicDBList();
			or.add(q1);
			or.add(q2);

			BasicDBObject query = new BasicDBObject("$or", or);

			DBCursor cursor = trendsColl.find(query);

			while (cursor.hasNext()) {
				DBObject o = cursor.next();
				trends.add(o.get("name").toString());
			}
		} catch (MongoException e) {
			Console.Log("Mongo Exception at Getting Active Trends");
			Console.Log(e.getMessage());

			Console.WriteExceptionDump(e, e.getCode());
		}

		return trends;
	}

	public void addStatus(String statusJson, String trends) {

		BasicDBObject status = (BasicDBObject) JSON.parse(statusJson);

		DBObject user = (BasicDBObject) status.get("user");
		Number id = (Number) user.get("id");

		status.append("trends", trends);
		status.append("index_id", id.toString() + "-" + System.currentTimeMillis());

		try {
			statusColl.insert(status);

		} catch (MongoException e) {
			Console.Log("Mongo Exception Inserting Status");
			Console.Log(e.getMessage());
		}
	}

	public void addStatus(String statusJson) {

		BasicDBObject status = (BasicDBObject) JSON.parse(statusJson);

		try {
			statusColl.insert(status);

		} catch (MongoException e) {
			Console.Log("Mongo Exception Inserting Status");
			Console.Log(e.getMessage());
		}
	}

	public void addUniqueTrend(long user_id, String trend) {

		BasicDBObject uq = new BasicDBObject("user", user_id).append("trend", trend);

		try {
			uniqueTrendsColl.insert(uq);
		} catch (MongoException e) {
			// Console.Log("Mongo Exception Inserting Unique Trend");
			// Console.Log(e.getMessage());
		}
	}

	public void addFollowedStatus(String statusJson) {
		BasicDBObject status = (BasicDBObject) JSON.parse(statusJson);

		try {
			followedColl.insert(status);

		} catch (MongoException e) {
			Console.Log("Mongo Exception Inserting Followed Status");
			Console.Log(e.getMessage());
		}
	}

	public void addGeneralUserInfo(long id, long age, int followers, int friends) {
		BasicDBObject user = new BasicDBObject("user", id).append("age", age).append("followers", followers).append("friends", friends);

		try {
			generalUserColl.insert(user);

		} catch (MongoException e) {
			Console.Log("Mongo Exception Inserting Followed Status");
			Console.Log(e.getMessage());
		}
	}

	public void addInspectedUserInfo(InspectedUser user) {

		BasicDBObject dbUser = new BasicDBObject();

		dbUser.append("user", user.getId());
		dbUser.append("tweets", user.getTweets());
		dbUser.append("retweets", user.getRetweets());
		dbUser.append("replies", user.getReplies());
		dbUser.append("mentions", user.getUserMentions());
		dbUser.append("retweeted", user.getRetweeted());
		dbUser.append("hashtags", user.getHashtags());
		dbUser.append("hashtagged", user.getHashtaggedTweets());
		dbUser.append("url", user.getUrlTweets());
		dbUser.append("retweets-mean", user.getRetweetPerTweetMean());
		dbUser.append("hashtagged-percent", user.getHashtaggedTweetsPercent());
		dbUser.append("url-percent", user.getUrlTweetsPercent());

		try {
			generalUserColl.insert(dbUser);

		} catch (MongoException e) {
			Console.Log("Mongo Exception Inserting Followed Status");
			Console.Log(e.getMessage());
		}
	}

	public DBCursor getStatusesCursor() {

		return statusColl.find();// .limit(100);
	}

	public DBCursor getStatusesCursor(BasicDBObject projection) {

		return statusColl.find(new BasicDBObject(), projection);// .limit(100);
	}

	public DBCursor getUniqueTrendsCursorSorted() {
		return uniqueTrendsColl.find().sort(new BasicDBObject("user", 1));
	}

	public DBCursor getFollowedStatusesCursor() {
		return followedColl.find();
	}

	public DBCursor getFollowedStatusesCursor(long userID) {
		return followedColl.find(new BasicDBObject("user.id", userID));
	}

	public long getStatuseColSize() {

		CommandResult com = statusColl.getStats();

		return com.getLong("size") / 1024 / 1024;
	}

	public List<Long> getUserIDs() {
		return statusColl.distinct("user.id");
	}

	public List<Number> getInspectedUserIDs() {
		return followedColl.distinct("user.id");
	}

}
