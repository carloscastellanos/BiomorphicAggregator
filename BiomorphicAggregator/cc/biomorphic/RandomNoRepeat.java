/**
 * 
 */
package cc.biomorphic;

/**
 * @author carlos
 *
 */
public class RandomNoRepeat
{
	/**
	 * 
	 * @param size the amount of numbers to return
	 * @param maxNumber the highes value a number can be
	 * @return an int array of the numbers
	 */
	public static int[] generate(int size, int maxNumber) {
		if(size >= maxNumber) {
			System.out.println("size must be less than maxNumber!");
			return null;
		}
		int[] randomInts = new int[size];
		for(int i = 0; i < randomInts.length; i++) {
			int x = (int)(Math.random()*maxNumber);
			while(contains(randomInts, x)) {
				x = (int)Math.random()*maxNumber;
			}
			randomInts[i] = x;
		}
		return randomInts;
	}
	
	private static boolean contains(int[] array, int val) {
		for(int i : array) {
			if(i == val) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param size the amount of numbers to return
	 * @param maxNumber the highes value a number can be
	 * @return a double array of the numbers
	 */
	public static double[] generate(int size, double maxNumber) {
		if(size >= maxNumber) {
			System.out.println("size must be less than maxNumber!");
			return null;
		}
		double[] randomDoubles = new double[size];
		for(int i = 0; i < randomDoubles.length; i++) {
			double x = (double)(Math.random()*maxNumber);
			while(contains(randomDoubles, x)) {
				x = (double)Math.random()*maxNumber;
			}
			randomDoubles[i] = x;
		}
		return randomDoubles;
	}
	
	private static boolean contains(double[] array, double val) {
		for(double i : array) {
			if(i == val) {
				return true;
			}
		}
		return false;
	}
	
}
