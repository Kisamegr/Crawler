package main;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;

import twitter4j.Trend;
import twitter4j.Trends;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class MongoDB {

	MongoClient mongoClient;

	ArrayList<Trend> oldTrends;

	public MongoDB() {

		try {

			mongoClient = new MongoClient("localhost");

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		oldTrends = new ArrayList<>();

	}

	public void addTrend(Trends trends) {

		Date now = new Date();

		DB db = mongoClient.getDB("twitter");

		DBCollection coll = db.getCollection("trends");

		for (Trend t : trends.getTrends()) {
			System.out.println(t.getName());
			coll.insert(new BasicDBObject("name", t.getName()).append("arrival", now).append("withdrawal", "not-finished"));
		}

		if (!oldTrends.isEmpty()) {
			for (Trend old : oldTrends) {
				for (Trend t : trends.getTrends()) {
					if (old.getName().compareTo(t.getName()) == 0) {

						BasicDBObject newDocument = new BasicDBObject();
						newDocument.append("$set", new BasicDBObject().append("withdrawal", now));

						BasicDBObject searchQuery = new BasicDBObject().append("name", old.getName());

						coll.update(searchQuery, newDocument);
						break;
					}

				}
			}
		}

		oldTrends.clear();
		for (Trend t : trends.getTrends()) {
			oldTrends.add(t);

		}

	}
}
