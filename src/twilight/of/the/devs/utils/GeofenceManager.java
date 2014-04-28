package twilight.of.the.devs.utils;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import twilight.of.the.devs.mylibrary.MARKER_STATE;
import twilight.of.the.devs.mylibrary.Marker;

/*
 * Manages Geofences
 */
public class GeofenceManager {
	
	private static final String TAG = GeofenceManager.class.getName();
	private HashMap<Marker, MarkerInfo> markerMap;
	private Context context;
	private long timeSinceLastCheck;
	
	/*
	 * MarkerInfo is used in the hashmap to keep track of markers that we have previously broadcasted.
	 * We only want to get notified of markers that we haven't previously been notified of.
	 */
	private class MarkerInfo {
		public long dwellTime;
		public boolean inside;
		public boolean hasBroadcasted;
		public MarkerInfo(long dwellTime, boolean inside){
			this.dwellTime = dwellTime;
			this.inside = inside;
		}
		public void addTime(long time){ dwellTime += time;}
	}

	/*
	 * Setup the geofencemanager with a list of markers.
	 */
	public GeofenceManager(List<Marker> markers, Context context){
		this.context = context;
		markerMap = new HashMap<Marker, MarkerInfo>();
		for(Marker marker : markers){
			markerMap.put(marker, new MarkerInfo(0L, false));
		}
		timeSinceLastCheck = System.currentTimeMillis();
	}
	
	/*
	 * Set the marker hashmap based on a list of markers
	 */
	public void setMarkerList(List<Marker> list){
		markerMap = new HashMap<Marker, MarkerInfo>();
		for(Marker marker : list){
			markerMap.put(marker, new MarkerInfo(0L, false));
		}
	}
	
	/*
	 * Checks whether a user is within the radius of a geofence
	 * @param location the user's current location
	 * Sends a broadcast depending of the user's location
	 */
	public void checkGeofences(Location location){
		
		//Get the current time so we can check for DWELLING state
		long currentTime = System.currentTimeMillis();
		long diff = currentTime - timeSinceLastCheck;
		synchronized (markerMap) {

			for(Marker marker : markerMap.keySet()){
				markerMap.get(marker).addTime(diff);
				boolean inside = isWithInGeofence(marker, location);
				
				//If the user is inside the radius and the user was not previously inside...
				if(inside && !markerMap.get(marker).inside){
					MARKER_STATE type = MARKER_STATE.ENTERED;
					markerMap.put(marker, new MarkerInfo(0L, true));
					Intent i = new Intent("geofence");
					i.putExtra("type", type);
					i.putExtra("marker", marker);
					LocalBroadcastManager.getInstance(context).sendBroadcast(i);
					
				} else if (!inside && markerMap.get(marker).inside){ //if the user is not inside but WAS previously...
					MARKER_STATE type = MARKER_STATE.EXITED;
					markerMap.put(marker, new MarkerInfo(0L, false));
					Intent i = new Intent("geofence");
					i.putExtra("type", type);
					i.putExtra("marker", marker);
					LocalBroadcastManager.getInstance(context).sendBroadcast(i);
					
				} else if (inside && markerMap.get(marker).inside && markerMap.get(marker).dwellTime > 5 * 1000 && !markerMap.get(marker).hasBroadcasted){
					//if the user is inside and the user WAS previously inside AND the user has been inside for at least 5 seconds and we haven't broadcasted yet...
					MARKER_STATE type = MARKER_STATE.DWELLING;
					markerMap.get(marker).hasBroadcasted = true;
					Intent i = new Intent("geofence");
					i.putExtra("type", type);
					i.putExtra("marker", marker);
					LocalBroadcastManager.getInstance(context).sendBroadcast(i);
				}
			}
		
		}
		timeSinceLastCheck = currentTime;
	}
	
	/*
	 * Check whether a user is inside of a marker radius
	 * @param marker a toury marker
	 * @param location the user's current location
	 */
	private boolean isWithInGeofence(Marker marker, Location location){
		
		Location markerLoc = new Location(location);
		markerLoc.setLatitude(marker.getMarkerLatitude());
		markerLoc.setLongitude(marker.getMarkerLongitude());
		float distance[] = {0.0f, 0.0f};
		Location.distanceBetween(location.getLatitude(), location.getLongitude(), marker.getMarkerLatitude(), marker.getMarkerLongitude(), distance);

		if(distance[0] < marker.getRadius()){
			return true;
		}
		
		return false;
	}
	
}
