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
import twitter4j.TwitterObjectFactory;
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
			arg0.printStackTrace();
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
		public void onStatus(final Status arg0) {
			final String s = TwitterObjectFactory.getRawJSON(arg0);
			threadPool.submit(new Runnable() {

				@Override
				public void run() {
					mongo.addStatus(s);
				}

			});
			// System.out.println(TwitterObjectFactory.getRawJSON(arg0));
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

		ConfigurationBuilder cbs = new ConfigurationBuilder();
		cbs.setPrettyDebugEnabled(true).setOAuthConsumerKey("3c1m6Ui0nK5UF9cDZ9ODOEKL6").setOAuthConsumerSecret("PszUJ0IepNaLVYVMjZxTtVDZHuk8lJvaDrv1ohN22ruTEHbjZD").setOAuthAccessToken("2693882078-1Kx8nF7IsYLMCxja44LCTOcHnKoyHTdEwGmUHgQ").setOAuthAccessTokenSecret("BrnTB3kn99vaoc3714NJmKJxbuWRIuJAtwjtFZWeiQssb");
		cbs.setJSONStoreEnabled(true);

		twitterStream = new TwitterStreamFactory(cbs.build()).getInstance();
		// twitterStream = new TwitterStreamFactory().getInstance();
		twitterStream.addListener(new StatusHandler());

		timer = new Timer();

		timer.scheduleAtFixedRate(new TimerTask() {

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

				mongo.updateTrends(trends, trends.getTrendAt());

				System.out.println("\n\n");

				ArrayList<String> activeTrends = mongo.getActiveTrends();

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

		}, 0, 60 * 1000);

	}

	public static void main(String[] args) {
		new Main();
	}

}
