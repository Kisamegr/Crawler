package main;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

public class Main {

	static int count;
	static boolean streamRunning;

	MongoDB mongo;
	Twitter twitter;
	TwitterStream twitterStream;

	Timer timer;

	ExecutorService threadPool;

	public class StatusHandler implements StatusListener {

		@Override
		public void onException(Exception arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onDeletionNotice(StatusDeletionNotice arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onScrubGeo(long arg0, long arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStallWarning(StallWarning arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatus(Status arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTrackLimitationNotice(int arg0) {
			// TODO Auto-generated method stub

		}

	}

	public Main() {

		// Initialize stuff
		count = 0;
		streamRunning = false;

		mongo = new MongoDB();
		threadPool = Executors.newCachedThreadPool();

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey("9aLVgExqED52hm7GX1O0eg3PB").setOAuthConsumerSecret("bdRzvT0TcBegzdQKvfX7Y33zUZiZWD8uLfGP74Usy2E6oQ5XW1").setOAuthAccessToken("2693882078-8AMpx5CuBIUy7ObGD22N8i6RfttdWxorTUi2xww").setOAuthAccessTokenSecret("ymun3YG1RlJi48TFPUW7ZJcg3LZmPn2UbXef63EGxD1km");

		twitter = new TwitterFactory(cb.build()).getInstance();

		twitterStream = new TwitterStreamFactory(cb.build()).getInstance();

		timer = new Timer();

		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {

				threadPool.submit(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub

						Trends trends = null;

						try {
							trends = twitter.getPlaceTrends(1);
						} catch (TwitterException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						System.out.println("----------- ITERATION " + count + " -----------");

						mongo.updateTrends(trends, trends.getTrendAt());

						System.out.println("\n\n");

						ArrayList<String> activeTrends = mongo.getActiveTrends();

						FilterQuery filter = new FilterQuery();
						filter.track(activeTrends.toArray(new String[activeTrends.size()]));

						count++;

					}

				});
			}

		}, 0, 60 * 1000);

	}

	public static void main(String[] args) {
		new Main();
	}

}
