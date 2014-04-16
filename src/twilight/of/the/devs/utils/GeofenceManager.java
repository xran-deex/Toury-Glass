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

public class GeofenceManager {
	
	private static final String TAG = GeofenceManager.class.getName();
	private HashMap<Marker, MarkerInfo> markerMap;
	private Context context;
	private long timeSinceLastCheck;
	
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

	public GeofenceManager(List<Marker> markers, Context context){
		this.context = context;
		markerMap = new HashMap<Marker, MarkerInfo>();
		for(Marker marker : markers){
			markerMap.put(marker, new MarkerInfo(0L, false));
		}
		timeSinceLastCheck = System.currentTimeMillis();
	}
	
	public void setMarkerList(List<Marker> list){
		markerMap = new HashMap<Marker, MarkerInfo>();
		for(Marker marker : list){
			markerMap.put(marker, new MarkerInfo(0L, false));
		}
	}
	
	public void checkGeofences(Location location){
		//Log.d(TAG, markerMap.toString());
		long currentTime = System.currentTimeMillis();
		long diff = currentTime - timeSinceLastCheck;
		synchronized (markerMap) {

			for(Marker marker : markerMap.keySet()){
				markerMap.get(marker).addTime(diff);
				boolean inside = isWithInGeofence(marker, location);
				if(inside && !markerMap.get(marker).inside){
					MARKER_STATE type = MARKER_STATE.ENTERED;
					markerMap.put(marker, new MarkerInfo(0L, true));
					Intent i = new Intent("geofence");
					i.putExtra("type", type);
					i.putExtra("marker", marker);
					LocalBroadcastManager.getInstance(context).sendBroadcast(i);
				} else if (!inside && markerMap.get(marker).inside){
					MARKER_STATE type = MARKER_STATE.EXITED;
					markerMap.put(marker, new MarkerInfo(0L, false));
					Intent i = new Intent("geofence");
					i.putExtra("type", type);
					i.putExtra("marker", marker);
					LocalBroadcastManager.getInstance(context).sendBroadcast(i);
				} else if (inside && markerMap.get(marker).inside && markerMap.get(marker).dwellTime > 5 * 1000 && !markerMap.get(marker).hasBroadcasted){
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
	
	private boolean isWithInGeofence(Marker marker, Location location){
		
		Location markerLoc = new Location(location);
		markerLoc.setLatitude(marker.getMarkerLatitude());
		markerLoc.setLongitude(marker.getMarkerLongitude());
		float distance[] = {0.0f, 0.0f};
		Location.distanceBetween(location.getLatitude(), location.getLongitude(), marker.getMarkerLatitude(), marker.getMarkerLongitude(), distance);
		//Log.d(TAG, "Distance to marker: " + distance[0]);
		//Log.d(TAG, "Marker radius " + marker.getRadius());
		if(distance[0] < marker.getRadius()){
			return true;
		}
		
		return false;
	}
	
}
