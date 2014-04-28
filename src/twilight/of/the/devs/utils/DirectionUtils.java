package twilight.of.the.devs.utils;

import android.location.Location;
import android.util.Log;

/*
 * Utilities for determining the user's direction
 */
public class DirectionUtils {

	public enum DIRECTION {LEFT, RIGHT, BEHIND}

	private static final String TAG = DirectionUtils.class.getName();
	
	/*
	 * Determines on which side of the user the current marker is.
	 * @param bearingTo the bearing to the marker we want to determine the side of
	 * @param currentHeading the user's currentHeading
	 */
	public static DIRECTION isOnWhichSide(double bearingTo, double currentHeading){
		double diff = Math.abs(bearingTo - currentHeading);
		if(bearingTo > 270 && currentHeading < 90){
			bearingTo = 360 - bearingTo;
			diff = bearingTo + currentHeading;
		} else if (bearingTo < 90 && currentHeading > 270){
			currentHeading = 360 - currentHeading;
			diff = currentHeading + bearingTo;
		}
		if(diff < 0) diff += 360;
		Log.d(TAG, "Diff " + diff);
		
		//Return left, right, or behind based on the previous calculations
		if(bearingTo < currentHeading && diff < 80) return DIRECTION.LEFT;
		else if (bearingTo > currentHeading && diff < 80) return DIRECTION.RIGHT;
		else return DIRECTION.BEHIND;
	}
	
	/*
	 * Converts a negative bearing to positive
	 * @param bearing the bearing
	 * @return the new (non-negative) bearing
	 */
	public static double convertBearing(double bearing){
		if(bearing >= 0) return bearing;
		return bearing + 360;
	}
	
	/* The following code is from the Android Compass Example Code */
	
	/**
     * Calculates {@code a mod b} in a way that respects negative values (for example,
     * {@code mod(-1, 5) == 4}, rather than {@code -1}).
     *
     * @param a the dividend
     * @param b the divisor
     * @return {@code a mod b}
     */
    public static float mod(float a, float b) {
        return (a % b + b) % b;
    }
	
	/**
     * Gets the relative bearing from one geographical coordinate to another.
     *
     * @param latitude1 the latitude of the source point
     * @param longitude1 the longitude of the source point
     * @param latitude2 the latitude of the destination point
     * @param longitude2 the longitude of the destination point
     * @return the relative bearing from point 1 to point 2, in degrees. The result is guaranteed
     *         to fall in the range 0-360
     */
    public static float getBearing(double latitude1, double longitude1, double latitude2,
            double longitude2) {
        latitude1 = Math.toRadians(latitude1);
        longitude1 = Math.toRadians(longitude1);
        latitude2 = Math.toRadians(latitude2);
        longitude2 = Math.toRadians(longitude2);

        double dLon = longitude2 - longitude1;

        double y = Math.sin(dLon) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1)
                * Math.cos(latitude2) * Math.cos(dLon);

        double bearing = Math.atan2(y, x);
        return mod((float) Math.toDegrees(bearing), 360.0f);
    }
}
