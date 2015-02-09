package main;

import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;
import util.MongoDB;

public abstract class StreamingAPI {

	static int count;
	static double statusesCount = 0;

	public Object streamLock;

	protected MongoDB mongo;
	protected Twitter twitter;
	protected TwitterStream twitterStream;

	protected long timerInterval;

	protected ScheduledExecutorService threadPool;

	protected Runnable mainRunnable;
	protected TimerTask task;

	protected long startTime;
	protected long totalPeriod;

	protected abstract class StatusHandler implements StatusListener {

		@Override
		public void onException(Exception arg0) {
			Console.Log("Exception at Twitter Stream (2)");
			Console.Log(arg0.getMessage());
			Console.WriteExceptionDump(arg0, 0);

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

		Console.Log("Intializing Streaming API");
		// Initialize stuff
		count = 0;
		streamLock = new Object();

		mongo = new MongoDB();
		threadPool = Executors.newScheduledThreadPool(2);

		ConfigurationBuilder cb = new ConfigurationBuilder();
		// Stratos key rest
		cb.setDebugEnabled(true).setOAuthConsumerKey("9aLVgExqED52hm7GX1O0eg3PB").setOAuthConsumerSecret("bdRzvT0TcBegzdQKvfX7Y33zUZiZWD8uLfGP74Usy2E6oQ5XW1").setOAuthAccessToken("2693882078-8AMpx5CuBIUy7ObGD22N8i6RfttdWxorTUi2xww").setOAuthAccessTokenSecret("ymun3YG1RlJi48TFPUW7ZJcg3LZmPn2UbXef63EGxD1km");

		// Tanaskey
		// cb.setDebugEnabled(true).setOAuthConsumerKey("sLCVaE4Md4rjm77re9MK95rnx").setOAuthConsumerSecret("ETwTOyW2GB5qMo2rvWw0p43zBkGXrz6UO45PKaiRDzLzDXM4pS").setOAuthAccessToken("842087294-hPnJnipEn3FigOekqTb18IT0H4QVe03wTn2ljn9p").setOAuthAccessTokenSecret("PnFIAoRSaPbsQbK2cwB35fpRYDtBV4uEbjYlu6E85Rb7V");

		twitter = new TwitterFactory(cb.build()).getInstance();
		ConfigurationBuilder cbs = new ConfigurationBuilder();

		// Stratos key streaming
		// cbs.setPrettyDebugEnabled(true).setOAuthConsumerKey("3c1m6Ui0nK5UF9cDZ9ODOEKL6").setOAuthConsumerSecret("PszUJ0IepNaLVYVMjZxTtVDZHuk8lJvaDrv1ohN22ruTEHbjZD").setOAuthAccessToken("2693882078-1Kx8nF7IsYLMCxja44LCTOcHnKoyHTdEwGmUHgQ").setOAuthAccessTokenSecret("BrnTB3kn99vaoc3714NJmKJxbuWRIuJAtwjtFZWeiQssb");

		cbs.setDebugEnabled(true).setOAuthConsumerKey("sLCVaE4Md4rjm77re9MK95rnx").setOAuthConsumerSecret("ETwTOyW2GB5qMo2rvWw0p43zBkGXrz6UO45PKaiRDzLzDXM4pS").setOAuthAccessToken("842087294-hPnJnipEn3FigOekqTb18IT0H4QVe03wTn2ljn9p").setOAuthAccessTokenSecret("PnFIAoRSaPbsQbK2cwB35fpRYDtBV4uEbjYlu6E85Rb7V");
		cbs.setJSONStoreEnabled(true);

		twitterStream = new TwitterStreamFactory(cbs.build()).getInstance();
		// twitterStream = new TwitterStreamFactory().getInstance();

		timerInterval = 30 * 1000;

	}

	public void StartStreaming() {
		threadPool.scheduleAtFixedRate(task, 0, timerInterval, TimeUnit.MILLISECONDS);
	}

	public void StopStreaming() {
		task.cancel();
		threadPool.shutdown();
		twitterStream.cleanUp();

		synchronized (streamLock) {
			streamLock.notify();
		}

	}

}
