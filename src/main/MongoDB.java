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
import com.mongodb.MongoException;

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

		DB db = mongoClient.getDB("twitter");

		DBCollection coll = db.getCollection("trends");

		coll.createIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));

		oldTrends = new ArrayList<>();

	}

	public void updateTrends(Trends trends, Date now) {

		DB db = mongoClient.getDB("twitter");

		DBCollection coll = db.getCollection("trends");

		System.out.println("Trends: ");
		for (Trend t : trends.getTrends()) {
			System.out.println(t.getName());

			try {
				coll.insert(new BasicDBObject("name", t.getName()).append("arrival", now).append("withdrawal", "not-finished"));
			} catch (MongoException e) {
			}
		}

		System.out.println("-=-=-=-=-=-=-=-=-");
		System.out.println("Old Trends:");

		for (Trend t : oldTrends) {
			System.out.println(t.getName());
		}

		System.out.println("-=-=-=-=-=-=-=-=-");

		if (!oldTrends.isEmpty()) {
			for (Trend old : oldTrends) {
				boolean found = false;
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

					coll.update(searchQuery, newDocument);

					System.out.println("Changing " + old.getName() + " to finished");
				}
			}
		}

		oldTrends.clear();
		for (Trend t : trends.getTrends()) {
			oldTrends.add(t);

		}

	}
}
