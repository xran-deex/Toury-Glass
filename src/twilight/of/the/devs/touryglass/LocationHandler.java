package twilight.of.the.devs.touryglass;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import twilight.of.the.devs.mylibrary.MARKER_STATE;
import twilight.of.the.devs.mylibrary.Marker;
import twilight.of.the.devs.mylibrary.MarkerOrderingComp;
import twilight.of.the.devs.utils.DirectionUtils;
import twilight.of.the.devs.utils.GeofenceManager;
import twilight.of.the.devs.utils.OrientationManager;
import twilight.of.the.devs.utils.DirectionUtils.DIRECTION;
import twilight.of.the.devs.utils.OrientationManager.OnChangedListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

public class LocationHandler {
	
	
	protected static final String TAG = LocationHandler.class.getName();

	private BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, final Intent intent) {
			Log.d(TAG, "Received location");
			//mTouryView.setRenderer(LocationHandler.this);
//			setMarkerList(((List<Marker>)intent.getSerializableExtra("loc")));
//			setMarkerList(mService.getCurrentMarkers());
			mTouryView.setMarkerList(mMarkerList);
			geofenceManager.setMarkerList(mMarkerList);
		}
	};
	
	private BroadcastReceiver mGeofenceReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, final Intent intent) {
			Log.d(TAG, "Received marker");
			
			MARKER_STATE state = (MARKER_STATE)intent.getSerializableExtra("type");
			Marker marker = (Marker)intent.getSerializableExtra("marker");
			//mTouryView.setRenderer(LocationHandler.this);
			if(state == MARKER_STATE.ENTERED){
				
				Location location = new Location("");
				location.setLatitude(marker.getMarkerLatitude());
				location.setLongitude(marker.getMarkerLongitude());
				double bearingTo = DirectionUtils.convertBearing(mCurrentLocation.bearingTo(location));
				Log.d(TAG, "BearingTo: " + bearingTo + ", Heading: " + mHeading);
				DIRECTION direction = DirectionUtils.isOnWhichSide(bearingTo, mHeading);
				if(direction == DIRECTION.LEFT){
					if(mSpeech != null)
						mSpeech.speak("There is something to look at on your left.", TextToSpeech.QUEUE_FLUSH, null);
				} else if (direction == DIRECTION.RIGHT){
					if(mSpeech != null)
						mSpeech.speak("There is something to look at on your right.", TextToSpeech.QUEUE_FLUSH, null);
				} else if (direction == DIRECTION.BEHIND){
					if(mSpeech != null)
						mSpeech.speak("There is something to look at behind you.", TextToSpeech.QUEUE_FLUSH, null);
				}
				mTouryView.setTriggeredMarker(marker);
			} else if (state == MARKER_STATE.DWELLING){
//				if(mService.getTTS() != null)
//					mService.getTTS().speak("You are dwelling in a geofence." + marker.getDescription(), TextToSpeech.QUEUE_FLUSH, null);
				Log.d(TAG, "Dwelling in " + marker.getTitle());
				mTouryView.setTriggeredMarker(marker);
				if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("ordered", false)){
					mMarkerList.remove(0);
					mTouryView.setMarkerList(mMarkerList);
					geofenceManager.setMarkerList(mMarkerList);
				}
			} else if (state == MARKER_STATE.EXITED){
//				if(mService.getTTS() != null)
//					mService.getTTS().speak("You have left a geofence.", TextToSpeech.QUEUE_FLUSH, null);
				mTouryView.removeTriggeredMarker();
			}
		}
	};

	private OrientationManager mOrientationManager;
	private Context mContext;
	private GeofenceManager geofenceManager;
	private List<Marker> mMarkerList;
	private TouryView mTouryView;
	protected float mHeading;
	protected Location mCurrentLocation;
	private TextToSpeech mSpeech;
	
	public LocationHandler(Context context, TouryView view){
		this.mContext = context;
		this.mTouryView = view;
		//LayoutInflater inflater = LayoutInflater.from(mContext);
		//FrameLayout layout = (FrameLayout)inflater.inflate(R.layout.activity_main, null);
		//mServerThread = new ServerThread(context);
		//mServerThread.start();
		LocalBroadcastManager.getInstance(context).registerReceiver(mReceiver, new IntentFilter("location"));
		//LocalBroadcastManager.getInstance(context).registerReceiver(mMarkerReceiver, new IntentFilter("marker"));
		LocalBroadcastManager.getInstance(context).registerReceiver(mGeofenceReceiver, new IntentFilter("geofence"));
		
		//mTouryView = (TouryView)layout.findViewById(R.id.heading);
		//mTouryView.setRenderer(this);
		//mTouryView.setService(mService);
		setMarkerList(new LinkedList<Marker>());
		geofenceManager = new GeofenceManager(mMarkerList, mContext);
		geofenceManager.setMarkerList(mMarkerList);
		SensorManager sensorManager =
                (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        LocationManager locationManager =
                (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        
    	mOrientationManager = new OrientationManager(sensorManager, locationManager);
    	mOrientationManager.addOnChangedListener(new OnChangedListener() {
			
			@Override
			public void onOrientationChanged(OrientationManager orientationManager) {
				mHeading = mOrientationManager.getHeading();
				mTouryView.setHeading(mHeading);
				mTouryView.setCurrentLocation(orientationManager.getLocation());
			}
			
			@Override
			public void onLocationChanged(OrientationManager orientationManager) {
				Log.d(TAG, "Location changed");
				mCurrentLocation = orientationManager.getLocation();
				mTouryView.setCurrentLocation(orientationManager.getLocation());
				geofenceManager.checkGeofences(orientationManager.getLocation());
			}
			
			@Override
			public void onAccuracyChanged(OrientationManager orientationManager) {
			}
		});
    	mSpeech = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Do nothing.
            }
        });
    	mOrientationManager.start();
	}
	
	public List<Marker> getMarkerList() {
		return mMarkerList;
	}

	public void setMarkerList(List<Marker> mMarkerList) {
		this.mMarkerList = mMarkerList;
		Log.d(TAG, "Before sort: " + this.mMarkerList.toString());
		Collections.sort(this.mMarkerList, new MarkerOrderingComp());
		Log.d(TAG, "After sort: " + this.mMarkerList.toString());
		mTouryView.setMarkerList(mMarkerList);
		if(geofenceManager != null){
			geofenceManager.setMarkerList(mMarkerList);
		}
		Log.d(TAG, mMarkerList.toString());
	}

}
