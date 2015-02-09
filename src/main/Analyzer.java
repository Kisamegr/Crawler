package main;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

import util.InspectedUser;
import util.MongoDB;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class Analyzer {

	MongoDB mongo;
	HashMap<Long, InspectedUser> userScore;

	private String basePath;

	public Analyzer() {

		Console.Log("----- Started Analyzer -----");

		basePath = System.getProperty("user.dir") + File.separator + "plots";
		File base = new File(basePath);
		if (!base.exists())
			base.mkdirs();

		userScore = new HashMap<>();

		mongo = new MongoDB();

		DBCursor cursor = mongo.getInspectedUsersCursor();

		Console.Log("Evaluating users...");
		while (cursor.hasNext()) {
			evaluateUser(InspectedUser.createInspectedUser(cursor.next()));
		}
		Console.Log("Finished Evaluating");

		ArrayList<Entry<Long, Double>> countList = new ArrayList<>();
		for (Entry<Long, InspectedUser> entry : userScore.entrySet()) {
			Map.Entry<Long, Double> e = new AbstractMap.SimpleEntry<Long, Double>(entry.getKey(), entry.getValue().getScore());
			countList.add(e);
		}

		sortScores(countList);

		for (Entry<Long, Double> e : countList) {
			System.out.println(e);
		}

		Console.Log("Making Scatterplots...");
		MakeScatterPlots();
		Console.Log("Making Sources Pie Chart");
		MakeSourcePieChart();
		Console.Log("Making General Users Scatterplot");
		MakeGeneralUserPlots();

		Console.Log("FINITO");
	}

	public void evaluateUser(InspectedUser user) {

		if (user.getFollowers() + user.getFriends() == 0)
			user.setReputationScore(0.5);
		else
			user.setReputationScore((1 - (double) user.getFollowers() / (double) (user.getFollowers() + user.getFriends())));

		user.setUrlScore(user.getUrlTweetsPercent());
		user.setHashtagScore(user.getHashtaggedTweetsPercent());
		user.setMentionScore(user.getMentionPercent());
		user.setDuplicateScore(user.getDuplicateRatio() * 2);
		user.setRetweetScore(user.getRetweetPercent());

		// Console.Log(user.getId() + "    :   REP " + reputation + " -  URL " +
		// urls + " -  HASH " + hashtags + " -  MEN " + mentions + " -  DUP " +
		// duplicates + " -  RITO " + retweeted);

		// System.out.println("ID: " + user.getId() + " NN: " + reputation);
		// score = (3 * (1 - reputation) + 2.4 * urls + 3.3 * hashtags + 1.9 *
		// mentions + 2.3 * duplicates + 2.1 * retweeted) / 15;

		// System.out.println(score);

		userScore.put(user.getId(), user);
	}

	public void MakeScatterPlots() {
		final ArrayList<Double> scores = new ArrayList<>();
		String name = "";

		File folder = new File(basePath + File.separator + "scatterplots");
		if (!folder.exists())
			folder.mkdirs();

		for (int q = 0; q < 7; q++) {
			int i = 0;
			XYSeries data = new XYSeries("Dataset");
			for (Entry<Long, InspectedUser> user : userScore.entrySet()) {

				switch (q) {
				case 0:
					data.add(i++, user.getValue().getScore());
					break;
				case 1:
					data.add(i++, user.getValue().getReputationScore());
					break;
				case 2:
					data.add(i++, user.getValue().getUrlScore());
					break;
				case 3:
					data.add(i++, user.getValue().getHashtagScore());
					break;
				case 4:
					data.add(i++, user.getValue().getMentionScore());
					break;
				case 5:
					data.add(i++, user.getValue().getDuplicateScore());
					break;
				case 6:
					data.add(i++, user.getValue().getRetweetScore());
					break;

				}
				scores.add(user.getValue().getScore());
			}

			XYSeriesCollection col = new XYSeriesCollection();
			col.addSeries(data);

			switch (q) {
			case 0:
				name = "Total Score";
				break;
			case 1:
				name = "Reputation Score";
				break;
			case 2:
				name = "URL Score";
				break;
			case 3:
				name = "Hashtag Score";
				break;
			case 4:
				name = "Mention Score";
				break;
			case 5:
				name = "Duplicates Score";
				break;
			case 6:
				name = "Retweet Score";
				break;

			}

			JFreeChart chart = ChartFactory.createScatterPlot("Twitter", "User ID", name, col, PlotOrientation.VERTICAL, false, false, false);

			chart.getXYPlot().setRenderer(new XYShapeRenderer() {

				@Override
				public Shape getItemShape(int row, int col) {
					if (scores.get(col) > InspectedUser.scoreThreshold)
						return ShapeUtilities.createDiagonalCross(4, 2);
					else
						return ShapeUtilities.createDiamond(5);
				}

				@Override
				public Paint getItemPaint(int row, int col) {

					if (scores.get(col) > InspectedUser.scoreThreshold)
						return Color.RED;
					else
						return Color.BLUE;

				}
			});

			int width = 1280; /* Width of the image */
			int height = 768; /* Height of the image */

			File imageFile = new File(basePath + File.separator + "scatterplots" + File.separator + "scatter" + q + "-" + name + ".png");

			try {
				ChartUtilities.saveChartAsPNG(imageFile, chart, width, height);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void MakeSourcePieChart() {

		String sourceName;
		HashMap<String, Integer> sourceCounterLegit = new HashMap<>();
		HashMap<String, Integer> sourceCounterScam = new HashMap<>();

		for (Entry<Long, InspectedUser> user : userScore.entrySet()) {
			sourceName = user.getValue().getMaxSource();

			if (user.getValue().getScore() < InspectedUser.scoreThreshold) {
				if (sourceCounterLegit.get(sourceName) != null)
					sourceCounterLegit.put(sourceName, sourceCounterLegit.get(sourceName) + 1);
				else
					sourceCounterLegit.put(sourceName, 1);
			} else {
				if (sourceCounterScam.get(sourceName) != null)
					sourceCounterScam.put(sourceName, sourceCounterScam.get(sourceName) + 1);
				else
					sourceCounterScam.put(sourceName, 1);
			}
		}

		DefaultPieDataset datasetLegit = new DefaultPieDataset();
		DefaultPieDataset datasetScam = new DefaultPieDataset();
		int other = 0;

		for (Entry<String, Integer> src : sourceCounterLegit.entrySet()) {
			if (src.getValue() > 2)
				datasetLegit.setValue(src.getKey(), src.getValue());
			else
				other++;
		}

		datasetLegit.setValue("Other Sources", other);

		for (Entry<String, Integer> src : sourceCounterScam.entrySet()) {
			datasetScam.setValue(src.getKey(), src.getValue());
		}

		JFreeChart chartLegit = ChartFactory.createPieChart("Sources", datasetLegit, true, true, false);
		JFreeChart chartScam = ChartFactory.createPieChart("Sources", datasetScam, true, true, false);

		chartLegit.setAntiAlias(true);
		chartScam.setAntiAlias(true);

		int width = 1280; /* Width of the image */
		int height = 768; /* Height of the image */

		File folder = new File(basePath + File.separator + "piecharts");
		if (!folder.exists())
			folder.mkdirs();

		File imageFileLegit = new File(basePath + File.separator + "piecharts" + File.separator + "pie_sources_legit.png");
		File imageFileScam = new File(basePath + File.separator + "piecharts" + File.separator + "pie_sources_scam.png");

		try {
			ChartUtilities.saveChartAsPNG(imageFileLegit, chartLegit, width, height);
			ChartUtilities.saveChartAsPNG(imageFileScam, chartScam, width, height);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void MakeGeneralUserPlots() {

		DBCursor cursor = mongo.getGeneralUsersCursor();

		XYSeries dataset = new XYSeries("");

		while (cursor.hasNext()) {
			DBObject user = cursor.next();
			int friends = (int) user.get("friends");
			int followers = (int) user.get("followers");
			dataset.add(friends, followers);
		}

		XYSeriesCollection col = new XYSeriesCollection();
		col.addSeries(dataset);

		JFreeChart chart = ChartFactory.createScatterPlot("Twitter", "Friends", "Followers", col, PlotOrientation.VERTICAL, false, false, false);

		chart.getXYPlot().setRenderer(new XYShapeRenderer() {
			@Override
			public Shape getItemShape(int row, int col) {

				return ShapeUtilities.createDiamond(4);
			}

			@Override
			public Paint getItemPaint(int row, int col) {

				return Color.BLUE;

			}
		});

		int width = 2000; /* Width of the image */
		int height = 1000; /* Height of the image */

		File imageFile = new File(basePath + File.separator + "general_users_scatterplot.png");

		try {
			ChartUtilities.saveChartAsPNG(imageFile, chart, width, height);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void sortScores(ArrayList<Entry<Long, Double>> countList) {

		Collections.sort(countList, new Comparator<Entry<Long, Double>>() {

			@Override
			public int compare(Entry<Long, Double> o1, Entry<Long, Double> o2) {

				if (o1.getValue() - o2.getValue() > 0)
					return 1;
				else if (o1.getValue() - o2.getValue() < 0)
					return -1;
				else
					return 0;

			}

		});
	}

	public static void main(String[] args) {
		new Analyzer();

	}

}
