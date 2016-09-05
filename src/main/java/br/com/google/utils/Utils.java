package br.com.google.utils;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {
	public static long logWatchStart() {
		long start = System.nanoTime();
		return start;
	}

	public static long logWatchStop(long start, String... params) {
		long stop = System.nanoTime();

		String methodName = Thread.currentThread().getStackTrace()[2]
				.getMethodName();
		String className = Thread.currentThread().getStackTrace()[2]
				.getClassName();
		Logger logger = Logger.getLogger(className);
		long convert = TimeUnit.MILLISECONDS.convert(stop - start,
				TimeUnit.NANOSECONDS);
		logger.log(Level.INFO,
				"Class: " + className + " :: " + Arrays.toString(params)
						+ " :: The method: " + methodName + " :: Time spend : "
						+ convert + "ms");

		return convert;
	}
}
