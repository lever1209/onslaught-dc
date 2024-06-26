package com.codetaylor.mc.onslaught.modules.onslaught.lib;

public final class Util {

	private Util() {
		//
	}

	/**
	 * Evaluates the input array as either a single, fixed value or two values: a
	 * min and max. Returns an array of two values, min and max.
	 *
	 * @param range the input array
	 * @return an array of two values, min and max
	 */
	public static int[] evaluateRangeArray(int[] range) {

		int[] result = new int[2];

		if (range.length == 1) {
			result[0] = range[0];
			result[1] = range[0];

		} else if (range.length > 1) {
			result[0] = Math.min(range[0], range[1]);
			result[1] = Math.max(range[0], range[1]);
		}

		return result;
	}
}
