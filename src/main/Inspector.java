package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TimerTask;

import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.TwitterObjectFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class Inspector extends StreamingAPI {

	private class StatusHanlderInspector extends StatusHandler {

		@Override
		public void onStatus(Status arg0) {
			final String json = TwitterObjectFactory.getRawJSON(arg0);
			threadPool.submit(new Runnable() {

				@Override
				public void run() {

					mongo.addStatus(json);
				}
			});
		}

	}

	private class TimerTaskInspector extends TimerTask {

		@Override
		public void run() {
			final FilterQuery filter = new FilterQuery();

			long[] users = new long[selectedUsers.size()];

			for (int i = 0; i < selectedUsers.size(); i++)
				users[i] = selectedUsers.get(i);

			filter.follow(users);

			threadPool.submit(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					twitterStream.filter(filter);

					// twitterStream.sample();
				}

			});
		}

	}

	ArrayList<Long> selectedUsers;

	public Inspector() {
		super();

		Random random = new Random(System.currentTimeMillis());

		DBCursor cursor = mongo.getStatusesCursor();

		HashMap<Number, Integer> countMap = new HashMap<>();

		for (int i = 0; i < 5000 && cursor.hasNext(); i++) {

			DBObject ob = cursor.next();
			DBObject user = (BasicDBObject) ob.get("user");
			Number id = (Number) user.get("id");

			if (countMap.containsKey(id))
				countMap.put(id, countMap.get(id) + 1);
			else
				countMap.put(id, 1);

		}

		// for (java.util.Map.Entry<Number, Integer> e : countMap.entrySet()) {
		// System.out.println(e.getKey() + "  -  " + e.getValue());
		// }

		System.out.println("Counted " + countMap.size() + " users.");
		System.out.println("Sorting the hash map...");

		ArrayList<Entry<Number, Integer>> countList = new ArrayList<>();

		countList.addAll(countMap.entrySet());

		Collections.sort(countList, new Comparator<Entry<Number, Integer>>() {

			@Override
			public int compare(Entry<Number, Integer> o1, Entry<Number, Integer> o2) {

				return o1.getValue() - o2.getValue();
			}

		});

		int Q1 = countList.size() / 4;
		int Q2 = countList.size() / 2;
		int Q3 = countList.size() * 3 / 4;

		System.out.println(countList.size() * 3 / 4);
		System.out.println("Q1 = " + countList.get(Q1).getValue());
		System.out.println("Q2 = " + countList.get(Q2).getValue());
		System.out.println("Q3 = " + countList.get(Q3).getValue());

		System.out.println("\nFirst = " + countList.get(0).getValue());
		System.out.println("Last = " + countList.get(countList.size() - 1).getValue());

		selectedUsers = new ArrayList<>();

		// Select 10 random users from each group
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 10; j++)
				selectedUsers.add((Long) countList.get(random.nextInt(Q1) + i * Q1).getKey());

	}

	public static void main(String[] args) {
		new Inspector();
	}
}
