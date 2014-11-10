package main;

import java.util.Timer;
import java.util.TimerTask;

import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class Main {

	static int count;
	MongoDB mongo;
	Twitter twitter;

	Timer timer;

	public Main() {

		count = 0;
		mongo = new MongoDB();

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey("9aLVgExqED52hm7GX1O0eg3PB").setOAuthConsumerSecret("bdRzvT0TcBegzdQKvfX7Y33zUZiZWD8uLfGP74Usy2E6oQ5XW1").setOAuthAccessToken("2693882078-8AMpx5CuBIUy7ObGD22N8i6RfttdWxorTUi2xww").setOAuthAccessTokenSecret("ymun3YG1RlJi48TFPUW7ZJcg3LZmPn2UbXef63EGxD1km");
		TwitterFactory tf = new TwitterFactory(cb.build());
		twitter = tf.getInstance();

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

				System.out.println("----------- ITERATION " + count + " -----------");
				mongo.updateTrends(trends, trends.getTrendAt());

				System.out.println("\n\n");

				count++;
			}

		}, 0, 60 * 1000);

	}

	public static void main(String[] args) {
		new Main();
	}

}
