package main;

import java.util.HashMap;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class Inspector {

	public Inspector() {

		MongoDB db = new MongoDB();
		DBCursor cursor = db.getStatusesCursor();

		HashMap<Number, Integer> countMap = new HashMap<>();

		for (int i = 0; i < 5000 && cursor.hasNext(); i++) {

			DBObject ob = cursor.next();
			DBObject user = (BasicDBObject) ob.get("user");
			Number id = (Number) user.get("id");

			if (countMap.containsKey(id))
				countMap.put(id, countMap.get(id) + 1);
			else
				countMap.put(id, 1);

		}

		for (java.util.Map.Entry<Number, Integer> e : countMap.entrySet()) {
			System.out.println(e.getKey() + "  -  " + e.getValue());
		}

	}

	public static void main(String[] args) {
		new Inspector();
	}
}
