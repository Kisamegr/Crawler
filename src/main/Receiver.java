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

	private class StatusHandlerReceiver extends StatusHandler {

		@Override
		public void onStatus(Status arg0) {

			final String json = TwitterObjectFactory.getRawJSON(arg0);
			threadPool.submit(new StatusRunnable(json, arg0.getText()));

		}

	}

	private class StatusRunnable implements Runnable {

		String json;
		String tweetText;

		public StatusRunnable(String json, String tweetText) {
			this.json = json;
			this.tweetText = tweetText;
		}

		@Override
		public void run() {
			String text = tweetText.toLowerCase();
			StringBuilder ctr = new StringBuilder();
			for (String t : activeTrends) {
				if (!t.startsWith("#") && text.contains(t.toLowerCase())) {
					ctr.append(t + "\t");
					// System.out.println(t);
				}
			}
			mongo.addStatus(json, ctr.toString());
			statusesCount++;
		}

	}

	private class TimerTaskReceiver extends TimerTask {

		@Override
		public void run() {
			Trends trends = null;

			Console.Log("");
			Console.Log("ITERATION " + count + "--------");
			Console.Log("Previous Statuses: " + statusesCount + "  Rate: " + statusesCount / 300);

			try {

				Console.Log("Fetching new trends");
				trends = twitter.getPlaceTrends(1);
			} catch (TwitterException e) {

				Console.Log("Failed to fetch new trends");
				Console.Log(e.getErrorMessage());

				Console.WriteExceptionDump(e, e.getErrorCode());

			}

			statusesCount = 0;

			mongo.updateTrends(trends, trends.getTrendAt());

			activeTrends = mongo.getActiveTrends();

			String[] array = new String[activeTrends.size()];
			array = activeTrends.toArray(array);

			final FilterQuery filter = new FilterQuery();
			filter.track(array);

			threadPool.submit(new Runnable() {

				@Override
				public void run() {

					try {
						twitterStream.filter(filter);
					} catch (Exception e) {
						Console.Log("Exception at Twitter Stream");
						Console.Log(e.getMessage());
						Console.WriteExceptionDump(e, 0);

					}

				}

			});
			Console.Log("| ACTIVE TRENDS");
			for (int i = 0; i < activeTrends.size(); i++) {
				Console.Log("|  " + activeTrends.get(i));
			}
			count++;

		}
	}

	public Receiver() {
		super();

		twitterStream.addListener(new StatusHandlerReceiver());

		Console.Log("Start Running");
		StartStreaming(new TimerTaskReceiver());

	}

	public static void main(String[] args) {
		new Receiver();
	}

}
