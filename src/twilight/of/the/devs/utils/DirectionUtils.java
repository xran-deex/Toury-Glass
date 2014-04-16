package twilight.of.the.devs.utils;

import android.location.Location;

public class DirectionUtils {

	public enum DIRECTION {LEFT, RIGHT, BEHIND};
	
	public static DIRECTION isOnWhichSide(double bearingTo, double currentHeading){
		double diff = Math.abs(bearingTo - currentHeading);
		if(bearingTo < currentHeading && diff < 110 && diff > 80) return DIRECTION.LEFT;
		else if (Math.abs(bearingTo - currentHeading) < 110 && diff > 80) return DIRECTION.RIGHT;
		else return DIRECTION.BEHIND;
	}
	
	public static double convertBearing(double bearing){
		if(bearing >= 0) return bearing;
		
		//return (Math.abs(bearing) + 180) % 360;
		return bearing + 360;
	}
	
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
