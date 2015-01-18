package main;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;

import org.bson.types.BasicBSONList;

import twitter4j.Trend;
import twitter4j.Trends;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
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

	ArrayList<Trend> oldTrends;

	public MongoDB() {

		try {

			mongoClient = new MongoClient("localhost");

			db = mongoClient.getDB("twitter");

			trendsColl = db.getCollection("trends");
			statusColl = db.getCollection("statuses");
			uniqueTrendsColl = db.getCollection("unique_trends");

			statusColl.createIndex(new BasicDBObject("index_id", "text"));

			trendsColl.createIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));
			uniqueTrendsColl.createIndex(new BasicDBObject("user", 1).append("trend", 1), new BasicDBObject("unique", true));

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
			BasicDBObject q2 = new BasicDBObject("withdrawal", new BasicDBObject("$gte", new Date(System.currentTimeMillis() - 1000 * 600)));

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

	public DBCursor getStatusesCursor() {

		return statusColl.find();// .limit(100);
	}

	public void testo() {

		DBCollection skatoules = db.getCollection("skata");

		DBCursor c = skatoules.find();

		for (int i = 0; i < c.size(); i++) {
			DBObject o = c.next();

			Object s = o.get("toules");

			System.out.println(i + ":  " + s);

			if (s instanceof BasicBSONList) {

				BasicBSONList t = (BasicBSONList) s;

				for (int gg = 0; gg < t.size(); gg++) {
					String epitelous = (String) ((DBObject) t.get(gg)).get("taf");
					System.out.println("TAF LIST:  " + epitelous);
				}

			}

		}

	}
}
