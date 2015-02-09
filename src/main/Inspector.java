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

	// Class to handle each status received from the Inspector
	private class StatusHanlderInspector extends StatusHandler {

		@Override
		public void onStatus(Status arg0) {
			// Get the JSON
			final String json = TwitterObjectFactory.getRawJSON(arg0);

			// Submit a new task to save the status into the mongo database
			threadPool.submit(new Runnable() {

				@Override
				public void run() {
					// Add status to mongo
					mongo.addFollowedStatus(json);
				}
			});
		}

	}

	// The main Inspector task that receives statuses based on the 40 randomly selected users
	private class TimerTaskInspector extends TimerTask {

		@Override
		public void run() {

			// Check if the total running time exceeds the total period
			// If so, end the stream
			if (System.currentTimeMillis() - startTime > totalPeriod) {
				Console.Log("Inspector Time has PASSED");
				Console.Log("Terminating Inspector Execution");

				StopStreaming();

				return;
			}

			Console.Log("----- Starting/Refreshing Inspector Streaming - Iteration " + count + " -----");

			// Initialize stuff
			count++;
			final FilterQuery filter = new FilterQuery();
			long[] users = new long[selectedUsers.size()];

			// Add the selected users from the Set to an Array
			Iterator<Long> it = selectedUsers.iterator();
			int i = 0;
			while (it.hasNext()) {
				users[i++] = it.next();
			}

			// Set the filter to follow those users
			filter.follow(users);

			Console.Log("Total Users: " + users.length);

			// Submit a new task for the filtering
			threadPool.submit(new Runnable() {

				@Override
				public void run() {
					// Start the filtering
					twitterStream.filter(filter);
				}

			});
		}
	}

	HashSet<Long> selectedUsers;

	public Inspector() {
		super();

		// Add the listener for the Streaming API
		twitterStream.addListener(new StatusHanlderInspector());

		Console.Log("Starting Inspector");

		// HashMap to hold each user's unique statuses number
		// UserID -> Uunique Counter
		HashMap<Long, Integer> countMap = new HashMap<>();

		// List to sort the above hashmap
		ArrayList<Entry<Long, Integer>> countList = new ArrayList<>();

		// The set that holds the 40 selected users
		selectedUsers = new HashSet<Long>();

		// Get the statuses cursor from mongo
		// In order to iterate all the statuses
		DBCursor cursor = mongo.getStatusesCursor();

		// Find unique trends and hashtags for each user...
		Console.Log("Finding each User's unique trends...");
		findUniqueTrends(cursor);

		// Get the unique trends found in the above function,
		// sorted by the user name
		DBCursor uniCursor = mongo.getUniqueTrendsCursorSorted();

		// Iterate on the unique trends and count them for each user
		Console.Log("Counting each User's unique trends...");
		countUniqueStatuses(uniCursor, countMap);

		Console.Log("Counted " + countMap.size() + " users.");
		Console.Log("Sorting the hash map...");

		// Sort the statuses based on the counter
		sortUniqueStatuses(countMap, countList);

		// Calculate the Q ranges
		int Q1 = countList.size() / 4;
		int Q2 = countList.size() / 2;
		int Q3 = countList.size() * 3 / 4;

		Console.Log("Inspector Stats:");
		Console.Log("  Q1 = " + countList.get(Q1).getValue());
		Console.Log("  Q2 = " + countList.get(Q2).getValue());
		Console.Log("  Q3 = " + countList.get(Q3).getValue());

		Console.Log("  First = " + countList.get(0).getValue());
		Console.Log("  Last  = " + countList.get(countList.size() - 1).getValue());

		// Select 10 random users from each group range
		Console.Log("Selecting and checking random users...");
		selectRandomUsers(countList, Q1);

		count = 0;

		// Set the total running period (default 7 days)
		totalPeriod = 1000 * 60 * 60 * 24 * 7;
		startTime = System.currentTimeMillis();

		// Initialize the task
		task = new TimerTaskInspector();

		// Start running
		StartStreaming();

	}

	// Find the unique trends for each user
	private void findUniqueTrends(DBCursor cursor) {

		// For each status in the Mongo Database
		for (int i = 0; i < cursor.size(); i++) {

			// Get the status JSON
			DBObject ob = cursor.next();

			// Get the user field in the JSON
			DBObject user = (BasicDBObject) ob.get("user");

			// Get the user ID
			Number id = (Number) user.get("id");

			// Get the entities filed from the JSON
			DBObject entities = (BasicDBObject) ob.get("entities");

			// Get the hashtags from the JSON
			BasicBSONList hashtags = (BasicBSONList) entities.get("hashtags");

			// Get the trends from our trends field in the JSON
			String trends_str = (String) ob.get("trends");

			// Split them
			String[] trends = trends_str.split("\t");

			// Add each trend into the mongo database.
			// Because the collection has a unique index based on the pair userID-trend,
			// the same userID-trend won't be in the database, effectively keeping only
			// the unique trends in the database
			for (String t : trends) {
				if (t != "")
					mongo.addUniqueTrend(id.longValue(), t);
			}

			// Same for the hashtags
			for (int n = 0; n < hashtags.size(); n++) {
				String text = (String) ((DBObject) hashtags.get(n)).get("text");
				if (text != "")
					mongo.addUniqueTrend(id.longValue(), text);

			}

		}
	}

	// Count the unique statuses for each user
	private void countUniqueStatuses(DBCursor uniCursor, HashMap<Long, Integer> countMap) {

		long userID = -1;

		// For each unique trend...
		for (int i = 0; i < uniCursor.size(); i++) {

			// Get the unique trend
			DBObject ob = uniCursor.next();

			// If the last user ID is different from the current, change it
			if (userID != (long) ob.get("user"))
				userID = (long) ob.get("user");

			// Add 1 to the count map
			if (!countMap.containsKey(userID))
				countMap.put(userID, 1);
			else
				countMap.put(userID, countMap.get(userID) + 1);

		}
	}

	// Sort the unique statuses counter list
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

		// For each group (4 groups)
		for (int i = 0; i < 4; i++) {

			// For at maximum 10 random users
			for (int j = 0; j < 10 && j < Q1; j++) {

				done = false;

				while (!done) {

					// Get a random user from the current range
					userIndex = random.nextInt(Q1) + i * Q1;

					userID = countList.get(userIndex).getKey();

					// If the selected user is not already selected
					if (!selectedUsers.contains(userID)) {

						try {

							// Search the user with the Twitter REST API
							// If the user does not exist (delete/banned user) it will
							// throw an exception here, and continue to select another random user
							twitter.showUser(userID);

							// The selected user exist
							// Add him to the selected users set
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
