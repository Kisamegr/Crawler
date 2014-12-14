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

	private class StatusHandlerReveiver extends StatusHandler {

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

			try {
				trends = twitter.getPlaceTrends(1);
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("\n\n\n\n\n----------- ITERATION " + count + " -----------");
			System.out.println("Previous Statuses: " + statusesCount + "  Rate: " + statusesCount / 60);
			statusesCount = 0;

			mongo.updateTrends(trends, trends.getTrendAt());

			System.out.println("\n\n");

			activeTrends = mongo.getActiveTrends();

			String[] array = new String[activeTrends.size()];
			array = activeTrends.toArray(array);

			final FilterQuery filter = new FilterQuery();
			filter.track(array);

			threadPool.submit(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					twitterStream.filter(filter);

					// twitterStream.sample();
				}

			});
			System.out.println("-=-=-=-=-=-= ACTIVE TRENDS -=-=-=-=-==-=-=-");
			for (int i = 0; i < activeTrends.size(); i++) {
				System.out.println(activeTrends.get(i));
			}
			count++;

		}

	}

	public Receiver() {
		super();

		twitterStream.addListener(new StatusHandlerReveiver());

		StartStreaming(new TimerTaskReceiver());

	}

	public static void main(String[] args) {
		new Receiver();
	}

}
