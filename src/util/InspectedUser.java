package util;

public class InspectedUser {
	private long id;
	private long tweets;
	private long retweets;
	private long replies;
	private long userMentions;
	private long retweeted;
	private long hashtags;
	private long hashtaggedTweets;
	private long urlTweets;
	private String maxSource;

	public InspectedUser() {
		tweets = 0;
		retweets = 0;
		replies = 0;
		userMentions = 0;
		retweeted = 0;
		hashtags = 0;
		hashtaggedTweets = 0;
		urlTweets = 0;
	}

	public double getRetweetPerTweetMean() {
		return ((double) (tweets + retweets)) / (double) retweeted;
	}

	public double getHashtaggedTweetsPercent() {
		return (double) hashtaggedTweets / (double) (tweets + retweets);
	}

	public double getUrlTweetsPercent() {
		return (double) urlTweets / (double) (tweets + retweets);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getTweets() {
		return tweets;
	}

	public void addTweets() {
		this.tweets++;
	}

	public long getRetweets() {
		return retweets;
	}

	public void addRetweets() {
		this.retweets++;
	}

	public long getReplies() {
		return replies;
	}

	public void addReplies() {
		this.replies++;
	}

	public long getUserMentions() {
		return userMentions;
	}

	public void addUserMentions(long userMentions) {
		this.userMentions += userMentions;
	}

	public long getRetweeted() {
		return retweeted;
	}

	public void addRetweeted(long retweeted) {
		this.retweeted += retweeted;
	}

	public long getHashtags() {
		return hashtags;
	}

	public void addHashtags(long hashtags) {
		this.hashtags += hashtags;
	}

	public long getHashtaggedTweets() {
		return hashtaggedTweets;
	}

	public void addHashtaggedTweets() {
		this.hashtaggedTweets++;
	}

	public long getUrlTweets() {
		return urlTweets;
	}

	public void addUrlTweets() {
		this.urlTweets++;
	}

	public String getMaxSource() {
		return maxSource;
	}

	public void setMaxSource(String maxSource) {
		this.maxSource = maxSource;
	}

	@Override
	public String toString() {

		return "User: " + id + " Max Source: " + maxSource;

	}
}
