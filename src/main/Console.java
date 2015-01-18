package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Console {

	public static void Log(String msg) {

		System.out.println(getDate() + msg);

	}

	public static String getDate() {
		return new SimpleDateFormat("[dd/MM/yyyy-HH:mm:ss] ").format(new Date());
	}

	public static void WriteExceptionDump(Exception e, int id) {

		String date = new SimpleDateFormat("dd_MM_yyyy-HH_mm_ss-").format(new Date());

		StringWriter stackTrace = new StringWriter();
		e.printStackTrace(new PrintWriter(stackTrace));

		String filename = date + id + "-" + System.currentTimeMillis() + ".dmp";
		File out = new File(System.getProperty("user.dir") + File.separator + "exception_dumps" + File.separator + filename);
		File folder = new File(System.getProperty("user.dir") + File.separator + "exception_dumps");

		try {
			if (!folder.exists())
				folder.mkdirs();

			out.createNewFile();
			BufferedWriter wr = new BufferedWriter(new FileWriter(out));
			wr.write(stackTrace.toString());
			wr.close();

			Log("Dumped exception at " + filename);
		} catch (IOException ex) {
			// TODO Auto-generated catch block
			Log("Could not write exception dump");
			Log(ex.getMessage());
		}
	}
}
