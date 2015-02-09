package util;

import com.mongodb.DBObject;

public class InspectedUser {
	private long id;
	private long friends;
	private long followers;
	private long age;
	private long tweets;
	private long retweets;
	private long replies;
	private long userMentions;
	private long hashtags;
	private long hashtaggedTweets;
	private long urlTweets;
	private String maxSource;
	private double duplicateRatio;

	private double score;
	private double reputationScore;
	private double urlScore;
	private double hashtagScore;
	private double mentionScore;
	private double duplicateScore;
	private double retweetScore;

	public static double scoreThreshold = 5;

	public InspectedUser() {
		tweets = 0;
		retweets = 0;
		replies = 0;
		userMentions = 0;
		hashtags = 0;
		hashtaggedTweets = 0;
		urlTweets = 0;
		duplicateRatio = 0;
		followers = 0;
		friends = 0;
		age = 0;
	}

	public double getRetweetPercent() {
		if (retweets + tweets == 0)
			return 0;

		return ((double) retweets) / (double) (retweets + tweets);
	}

	public double getHashtaggedTweetsPercent() {
		if (tweets + retweets == 0)
			return 0;

		return (double) hashtaggedTweets / (double) (tweets + retweets);
	}

	public double getUrlTweetsPercent() {
		if (tweets + retweets == 0)
			return 0;

		return (double) urlTweets / (double) (tweets + retweets);
	}

	public double getMentionPercent() {
		if (userMentions == 0 && tweets + retweets == 0)
			return 0;

		return (double) userMentions / (double) (userMentions + tweets + retweets);
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

	public void addTweets(long t) {
		this.tweets += t;
	}

	public long getRetweets() {
		return retweets;
	}

	public void addRetweets() {
		this.retweets++;
	}

	public void addRetweets(long r) {
		this.retweets += r;
	}

	public long getReplies() {
		return replies;
	}

	public void addReplies() {
		this.replies++;
	}

	public void addReplies(long r) {
		this.replies += r;
	}

	public long getUserMentions() {
		return userMentions;
	}

	public void addUserMentions(long userMentions) {
		this.userMentions += userMentions;
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

	public void addHashtaggedTweets(long h) {
		this.hashtaggedTweets += h;
	}

	public long getUrlTweets() {
		return urlTweets;
	}

	public void addUrlTweets() {
		this.urlTweets++;
	}

	public void addUrlTweets(long u) {
		this.urlTweets += u;
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

	public double getDuplicateRatio() {
		return duplicateRatio;
	}

	public void setDuplicateRatio(double duplicateRatio) {
		this.duplicateRatio = duplicateRatio;
	}

	public static InspectedUser createInspectedUser(DBObject obj) {
		InspectedUser user = new InspectedUser();

		// System.out.println(obj.toString());

		user.setId((long) obj.get("user"));
		user.setFollowers((long) obj.get("followers"));
		user.setFriends((long) obj.get("friends"));
		user.setAge((long) obj.get("age"));
		user.addTweets((long) obj.get("tweets"));
		user.addRetweets((long) obj.get("retweets"));
		user.addReplies((long) obj.get("replies"));
		user.addUserMentions((long) obj.get("mentions"));
		user.addHashtags((long) obj.get("hashtags"));
		user.addHashtaggedTweets((long) obj.get("hashtagged"));
		user.addUrlTweets((long) obj.get("url"));
		user.setDuplicateRatio((double) obj.get("dup-ratio"));
		user.setMaxSource((String) obj.get("source"));

		return user;
	}

	public long getFriends() {
		return friends;
	}

	public void setFriends(long friends) {
		this.friends = friends;
	}

	public long getFollowers() {
		return followers;
	}

	public void setFollowers(long followers) {
		this.followers = followers;
	}

	public long getAge() {
		return age;
	}

	public void setAge(long age) {
		this.age = age;
	}

	public double getScore() {
		score = (reputationScore + urlScore + hashtagScore + mentionScore + duplicateScore + retweetScore);
		return score;
	}

	public double getReputationScore() {
		return reputationScore;
	}

	public void setReputationScore(double reputationScore) {
		this.reputationScore = reputationScore;
	}

	public double getUrlScore() {
		return urlScore;
	}

	public void setUrlScore(double urlScore) {
		this.urlScore = urlScore;
	}

	public double getHashtagScore() {
		return hashtagScore;
	}

	public void setHashtagScore(double hashtagScore) {
		this.hashtagScore = hashtagScore;
	}

	public double getMentionScore() {
		return mentionScore;
	}

	public void setMentionScore(double mentionScore) {
		this.mentionScore = mentionScore;
	}

	public double getDuplicateScore() {
		return duplicateScore;
	}

	public void setDuplicateScore(double duplicateScore) {
		this.duplicateScore = duplicateScore;
	}

	public double getRetweetScore() {
		return retweetScore;
	}

	public void setRetweetScore(double retweetScore) {
		this.retweetScore = retweetScore;
	}
}
