package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TimerTask;

import org.bson.types.BasicBSONList;

import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.TwitterException;
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
					mongo.addFollowedStatus(json);
				}
			});
		}

	}

	private class TimerTaskInspector extends TimerTask {

		@Override
		public void run() {

			if (System.currentTimeMillis() - startTime > totalPeriod) {
				Console.Log("Inspector Time has PASSED");
				Console.Log("Terminating Inspector Execution");

				StopStreaming();

				return;
			}

			Console.Log("----- Starting/Refreshing Inspector Streaming - Iteration " + count + " -----");

			count++;
			final FilterQuery filter = new FilterQuery();

			long[] users = new long[selectedUsers.size()];

			Iterator<Long> it = selectedUsers.iterator();
			int i = 0;
			while (it.hasNext()) {
				users[i++] = it.next();
			}

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

	HashSet<Long> selectedUsers;

	public Inspector() {
		super();

		twitterStream.addListener(new StatusHanlderInspector());

		Console.Log("Starting Inspector");

		HashMap<Long, Integer> countMap = new HashMap<>();
		ArrayList<Entry<Long, Integer>> countList = new ArrayList<>();
		selectedUsers = new HashSet<Long>();

		DBCursor cursor = mongo.getStatusesCursor();

		Console.Log("Finding each User's unique trends...");
		findUniqueTrends(cursor);

		DBCursor uniCursor = mongo.getUniqueTrendsCursorSorted();

		Console.Log("Counting each User's unique trends...");
		countUniqueStatuses(uniCursor, countMap);

		Console.Log("Counted " + countMap.size() + " users.");
		Console.Log("Sorting the hash map...");

		sortUniqueStatuses(countMap, countList);

		int Q1 = countList.size() / 4;
		int Q2 = countList.size() / 2;
		int Q3 = countList.size() * 3 / 4;

		Console.Log("Inspector Stats:");
		Console.Log("  Q1 = " + countList.get(Q1).getValue());
		Console.Log("  Q2 = " + countList.get(Q2).getValue());
		Console.Log("  Q3 = " + countList.get(Q3).getValue());

		Console.Log("  First = " + countList.get(0).getValue());
		Console.Log("  Last  = " + countList.get(countList.size() - 1).getValue());

		Console.Log("Selecting and checking random users...");
		selectRandomUsers(countList, Q1);

		count = 0;
		totalPeriod = 1000 * 60 * 60 * 24 * 7;

		task = new TimerTaskInspector();

		startTime = System.currentTimeMillis();
		StartStreaming();

	}

	private void findUniqueTrends(DBCursor cursor) {

		for (int i = 0; i < cursor.size(); i++) {

			DBObject ob = cursor.next();
			DBObject user = (BasicDBObject) ob.get("user");
			Number id = (Number) user.get("id");

			DBObject entities = (BasicDBObject) ob.get("entities");
			BasicBSONList hashtags = (BasicBSONList) entities.get("hashtags");

			String trends_str = (String) ob.get("trends");

			String[] trends = trends_str.split("\t");

			for (String t : trends) {
				if (t != "")
					mongo.addUniqueTrend(id.longValue(), t);
			}

			for (int n = 0; n < hashtags.size(); n++) {
				String text = (String) ((DBObject) hashtags.get(n)).get("text");
				if (text != "")
					mongo.addUniqueTrend(id.longValue(), text);

				mongo.addUniqueTrend(id.longValue(), "ARIBARIBARIBARIBA");
			}

		}
	}

	private void countUniqueStatuses(DBCursor uniCursor, HashMap<Long, Integer> countMap) {

		long userID = -1;
		for (int i = 0; i < uniCursor.size(); i++) {

			DBObject ob = uniCursor.next();

			if (userID != (long) ob.get("user"))
				userID = (long) ob.get("user");

			// System.out.println("User: " + userID);
			if (!countMap.containsKey(userID))
				countMap.put(userID, 1);
			else
				countMap.put(userID, countMap.get(userID) + 1);

		}
	}

	private void sortUniqueStatuses(HashMap<Long, Integer> countMap, ArrayList<Entry<Long, Integer>> countList) {

		countList.addAll(countMap.entrySet());

		Collections.sort(countList, new Comparator<Entry<Long, Integer>>() {

			@Override
			public int compare(Entry<Long, Integer> o1, Entry<Long, Integer> o2) {

				return o1.getValue() - o2.getValue();
			}

		});
	}

	private void selectRandomUsers(ArrayList<Entry<Long, Integer>> countList, int Q1) {
		// Select 10 random users from each group

		Random random = new Random(System.currentTimeMillis());

		int userIndex;
		long userID;
		boolean done;
		for (int i = 0; i < 4; i++) {

			for (int j = 0; j < 10 && j < Q1; j++) {

				done = false;

				while (!done) {
					userIndex = random.nextInt(Q1) + i * Q1;
					userID = countList.get(userIndex).getKey();

					if (!selectedUsers.contains(userID)) {

						try {
							twitter.showUser(userID);

							selectedUsers.add(userID);

							done = true;

						} catch (TwitterException e) {

							if (e.isCausedByNetworkIssue()) {
								Console.Log("Error looking up a User @Inspector");
								Console.WriteExceptionDump(e, e.getErrorCode());

								try {
									Thread.sleep(1000);
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}

						}
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		new Inspector();

	}
}
