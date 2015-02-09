package main;

import java.util.ArrayList;
import java.util.TimerTask;

import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.Trends;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

public class Receiver extends StreamingAPI {

	protected ArrayList<String> activeTrends;

	// Class that extends the Status Handler and overrides the onStatus method
	// in order to manage each status received from the Streaming API
	private class StatusHandlerReceiver extends StatusHandler {

		@Override
		public void onStatus(Status arg0) {

			// Get the JSON
			final String json = TwitterObjectFactory.getRawJSON(arg0);

			// Add a new task to the threadpool in order to analyze and save the
			// status
			threadPool.submit(new StatusRunnable(json, arg0.getText()));

		}

	}

	// Class to analyze a status received
	private class StatusRunnable implements Runnable {

		String json;
		String tweetText;

		public StatusRunnable(String json, String tweetText) {
			this.json = json;
			this.tweetText = tweetText;
		}

		@Override
		public void run() {
			// Transform the text to lower case
			String text = tweetText.toLowerCase();

			StringBuilder ctr = new StringBuilder();

			// Because the Streaming API does not include the
			// referred in the JSON
			// Find and save the active trends that exist in the status text in
			// the Mongo, after the JSOn
			for (String t : activeTrends) {
				if (!t.startsWith("#") && text.contains(t.toLowerCase())) {
					ctr.append(t + "\t");
					// System.out.println(t);
				}
			}

			// Save status to mongo
			mongo.addStatus(json, ctr.toString());
			statusesCount++;
		}

	}

	// This is the main task for the Receiver Streaming API
	// It executes every timeInterval milliseconds (default 5 minutes)
	private class TimerTaskReceiver extends TimerTask {

		@Override
		public void run() {

			// Check if the total period of running (default 3 days) has passed
			// If so, end the streaming
			if (System.currentTimeMillis() - startTime > totalPeriod) {
				Console.Log("Receiver Time has PASSED");
				Console.Log("Terminating Receiver Execution");

				StopStreaming();
				return;
			}

			long dbSize = mongo.getStatuseColSize();

			// Check if the database size exceeds the 32Gb size (due to hard
			// drive size limitations)
			// If so, end the streaming
			if (dbSize > 32000) {
				Console.Log("Receiver Maximum Storage has reached its LIMIT");
				Console.Log("Terminating Receiver Execution");

				StopStreaming();
				return;
			}

			Trends trends = null;

			Console.Log("");
			Console.Log("-------- ITERATION " + count + " --------");
			Console.Log("Statuses Collection Size: " + dbSize + "mb");
			Console.Log("Previous Statuses: " + statusesCount + "  Rate: " + statusesCount / 300);

			// Fetch the current trends from Twitter using the REST API
			try {
				Console.Log("Fetching new trends");
				trends = twitter.getPlaceTrends(1);
			} catch (TwitterException e) {
				Console.Log("Failed to fetch new trends");
				Console.Log(e.getErrorMessage());
				Console.WriteExceptionDump(e, e.getErrorCode());
			}

			statusesCount = 0;

			// Send the trends to the Mongo Class, in order to insert the new
			// trends into the database,
			// and update the old trends variable
			mongo.updateTrends(trends, trends.getTrendAt());

			// Get the active trends from the Mongo Database
			// A trend is active when is still hasn't expired (withdrawal slot
			// it "not-finished)
			// and if the expiration period is less than 2 hours ago
			activeTrends = mongo.getActiveTrends();

			// Set the active trends to a String Array
			String[] array = new String[activeTrends.size()];
			array = activeTrends.toArray(array);

			// Initialize the filterring
			final FilterQuery filter = new FilterQuery();

			// Set the filtering to track all the statuses that refer to the
			// trends array
			filter.track(array);

			// Start a new thread for the filtering
			threadPool.submit(new Runnable() {

				@Override
				public void run() {

					try {
						// Start the filtering
						twitterStream.filter(filter);

					} catch (Exception e) {
						Console.Log("Exception at Twitter Stream");
						Console.Log(e.getMessage());
						Console.WriteExceptionDump(e, 0);
					}
				}

			});

			// Print the active trends
			Console.Log("| ACTIVE TRENDS");
			for (int i = 0; i < activeTrends.size(); i++) {
				Console.Log("|  " + activeTrends.get(i));
			}

			count++;

		}
	}

	public Receiver() {
		super();

		// Set the streaming listener
		twitterStream.addListener(new StatusHandlerReceiver());

		Console.Log("Start Running");

		// Initialize the task
		task = new TimerTaskReceiver();

		// Set the ending variables
		totalPeriod = 1000 * 60 * 60 * 24 * 3;
		startTime = System.currentTimeMillis();

		// Start running!
		StartStreaming();

	}

	public static void main(String[] args) {
		new Receiver();
	}

}
