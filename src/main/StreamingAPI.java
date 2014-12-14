package main;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

public abstract class StreamingAPI {

	static int count;
	static boolean streamRunning;
	static double statusesCount = 0;

	protected MongoDB mongo;
	protected Twitter twitter;
	protected TwitterStream twitterStream;

	protected Timer timer;
	protected long timerInterval;

	protected ExecutorService threadPool;

	protected Runnable mainRunnable;

	protected abstract class StatusHandler implements StatusListener {

		@Override
		public void onException(Exception arg0) {
			// TODO Auto-generated method stub
			arg0.printStackTrace();
		}

		@Override
		public abstract void onStatus(final Status arg0);

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
		public void onTrackLimitationNotice(int arg0) {
			// TODO Auto-generated method stub

		}

	}

	public StreamingAPI() {

		// Initialize stuff
		count = 0;
		streamRunning = false;

		mongo = new MongoDB();
		threadPool = Executors.newFixedThreadPool(4);

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey("9aLVgExqED52hm7GX1O0eg3PB").setOAuthConsumerSecret("bdRzvT0TcBegzdQKvfX7Y33zUZiZWD8uLfGP74Usy2E6oQ5XW1").setOAuthAccessToken("2693882078-8AMpx5CuBIUy7ObGD22N8i6RfttdWxorTUi2xww").setOAuthAccessTokenSecret("ymun3YG1RlJi48TFPUW7ZJcg3LZmPn2UbXef63EGxD1km");

		twitter = new TwitterFactory(cb.build()).getInstance();

		ConfigurationBuilder cbs = new ConfigurationBuilder();
		cbs.setPrettyDebugEnabled(true).setOAuthConsumerKey("3c1m6Ui0nK5UF9cDZ9ODOEKL6").setOAuthConsumerSecret("PszUJ0IepNaLVYVMjZxTtVDZHuk8lJvaDrv1ohN22ruTEHbjZD").setOAuthAccessToken("2693882078-1Kx8nF7IsYLMCxja44LCTOcHnKoyHTdEwGmUHgQ").setOAuthAccessTokenSecret("BrnTB3kn99vaoc3714NJmKJxbuWRIuJAtwjtFZWeiQssb");
		cbs.setJSONStoreEnabled(true);

		twitterStream = new TwitterStreamFactory(cbs.build()).getInstance();
		// twitterStream = new TwitterStreamFactory().getInstance();

		timer = new Timer();
		timerInterval = 60 * 1000;

	}

	public void StartStreaming(TimerTask t) {
		streamRunning = true;
		timer.scheduleAtFixedRate(t, 0, timerInterval);
	}

	public void StopStreaming() {
		if (streamRunning)
			timer.cancel();
	}
}
