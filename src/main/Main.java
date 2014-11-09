package main;

import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		MongoDB mongo = new MongoDB();

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		.setOAuthConsumerKey("9aLVgExqED52hm7GX1O0eg3PB")
		.setOAuthConsumerSecret(
				"bdRzvT0TcBegzdQKvfX7Y33zUZiZWD8uLfGP74Usy2E6oQ5XW1")
				.setOAuthAccessToken(
						"2693882078-8AMpx5CuBIUy7ObGD22N8i6RfttdWxorTUi2xww")
						.setOAuthAccessTokenSecret(
								"ymun3YG1RlJi48TFPUW7ZJcg3LZmPn2UbXef63EGxD1km");
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();

		Trends trends = null;

		try {
			trends = twitter.getPlaceTrends(1);
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mongo.addTrend(trends);

	}

}
