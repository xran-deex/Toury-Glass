package twilight.of.the.devs.touryglass;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import twilight.of.the.devs.utils.DirectionUtils;
import twilight.of.the.devs.utils.DirectionUtils.DIRECTION;
import twilight.of.the.devs.utils.GeofenceManager;
import twilight.of.the.devs.utils.OrientationManager;
import twilight.of.the.devs.utils.OrientationManager.OnChangedListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;

import com.google.android.glass.timeline.DirectRenderingCallback;
import twilight.of.the.devs.mylibrary.*;

/*
 * Renderer for the LiveCard
 */
public class LiveCardRenderer implements DirectRenderingCallback {

    // About 30 FPS.
    private static final long FRAME_TIME_MILLIS = 33;

	protected static final String TAG = LiveCardRenderer.class.getName();

    private SurfaceHolder mHolder;
    private boolean mPaused;
    private RenderThread mRenderThread;
    private float mHeading;
	private OrientationManager mOrientationManager;
	private Location mCurrentLocation;
	private TouryView mTouryView;
	private List<Marker> mMarkerList;
	private TouryService mService;
	
	private Context mContext;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, final Intent intent) {
			Log.d(TAG, "Received location");
			mTouryView.setRenderer(LiveCardRenderer.this);
			mTouryView.setMarkerList(mMarkerList);
			geofenceManager.setMarkerList(mMarkerList);
		}
	};
	
	/*
	 * Receives broadcasts of triggered geofences.
	 */
	private BroadcastReceiver mGeofenceReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, final Intent intent) {
			Log.d(TAG, "Received marker");
			
			//Check the geofence state -> DWELLING, ENTERED, or EXITED
			MARKER_STATE state = (MARKER_STATE)intent.getSerializableExtra("type");
			Marker marker = (Marker)intent.getSerializableExtra("marker");
			mTouryView.setRenderer(LiveCardRenderer.this);
			
			//If the state is entered, notify the user to look in its direction and set the triggered marker
			if(state == MARKER_STATE.ENTERED){
				Location location = new Location("");
				location.setLatitude(marker.getMarkerLatitude());
				location.setLongitude(marker.getMarkerLongitude());
				double bearingTo = DirectionUtils.convertBearing(mCurrentLocation.bearingTo(location));
				Log.d(TAG, "BearingTo: " + bearingTo + ", Heading: " + mHeading);
				DIRECTION direction = DirectionUtils.isOnWhichSide(bearingTo, mHeading);
				if(direction == DIRECTION.LEFT){
					if(mService.getTTS() != null)
						mService.getTTS().speak("There is something to look at on your left.", TextToSpeech.QUEUE_ADD, null);
				} else if (direction == DIRECTION.RIGHT){
					if(mService.getTTS() != null)
						mService.getTTS().speak("There is something to look at on your right.", TextToSpeech.QUEUE_ADD, null);
				} else if (direction == DIRECTION.BEHIND){
					if(mService.getTTS() != null)
						mService.getTTS().speak("There is something to look at behind you.", TextToSpeech.QUEUE_ADD, null);
				}
				mTouryView.setTriggeredMarker(marker);
				
			} else if (state == MARKER_STATE.DWELLING){ //if the state is DWELLING, show the description on the screen
//				if(mService.getTTS() != null)
//					mService.getTTS().speak("You are dwelling in a geofence." + marker.getDescription(), TextToSpeech.QUEUE_FLUSH, null);
				Log.d(TAG, "Dwelling in " + marker.getTitle());
				mTouryView.setTriggeredMarker(marker);
				if(PreferenceManager.getDefaultSharedPreferences(mService).getBoolean("ordered", false)){
					mMarkerList.remove(0);
					mService.getTTS().speak("Your next destination is " + mMarkerList.get(0).getTitle(), TextToSpeech.QUEUE_ADD, null);
					mTouryView.setMarkerList(mMarkerList);
					geofenceManager.setMarkerList(mMarkerList);
				}
			} else if (state == MARKER_STATE.EXITED){ //if state is EXIT, remove the marker
//				if(mService.getTTS() != null)
//					mService.getTTS().speak("You have left a geofence.", TextToSpeech.QUEUE_FLUSH, null);
				mTouryView.removeTriggeredMarker();
			}
		}
	};

	private GeofenceManager geofenceManager;
	
	public LiveCardRenderer(TouryService context) {
		this.mContext = context;
		this.mService = context;
		LayoutInflater inflater = LayoutInflater.from(mContext);
		FrameLayout layout = (FrameLayout)inflater.inflate(R.layout.activity_main, null);
		LocalBroadcastManager.getInstance(context).registerReceiver(mReceiver, new IntentFilter("location"));
		LocalBroadcastManager.getInstance(context).registerReceiver(mGeofenceReceiver, new IntentFilter("geofence"));
		
		//Create the view
		mTouryView = (TouryView)layout.findViewById(R.id.heading);
		mTouryView.setRenderer(this);
		mTouryView.setService(mService);
		
		setMarkerList(new LinkedList<Marker>());
		
		//Set up the geofence manager
		geofenceManager = new GeofenceManager(mMarkerList, mService);
		geofenceManager.setMarkerList(mMarkerList);
		
		//Set up the sensor and location managers
		SensorManager sensorManager =
                (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        LocationManager locationManager =
                (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        
        //Set up the orientation tracking
    	mOrientationManager = new OrientationManager(sensorManager, locationManager);
    	mOrientationManager.addOnChangedListener(new OnChangedListener() {
			
			@Override
			public void onOrientationChanged(OrientationManager orientationManager) {
				//Set the headings and locations
				mHeading = mOrientationManager.getHeading();
				mTouryView.setHeading(mHeading);
				mTouryView.setCurrentLocation(orientationManager.getLocation());
			}
			
			@Override
			public void onLocationChanged(OrientationManager orientationManager) {
				Log.d(TAG, "Location changed");
				//Reset the location when it changes
				mCurrentLocation = orientationManager.getLocation();
				mTouryView.setCurrentLocation(orientationManager.getLocation());
				geofenceManager.checkGeofences(orientationManager.getLocation());
			}
			
			@Override
			public void onAccuracyChanged(OrientationManager orientationManager) {
			}
		});
    	mOrientationManager.start();
	}
	
	public boolean isWithinTenDegrees(int one, int two){
		return Math.abs(one - two) <= 10;
	}
	
	/*
	 * Get the current TouryView
	 */
	public TouryView getTouryView(){
		return mTouryView;
	}
	
	
	//Surface callbacks...
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    	
        mHolder = holder;
        mRenderThread = new RenderThread();
        mRenderThread.start();
//        updateRendering();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder = null;

        mRenderThread.quit();
        mRenderThread = null;
        //mOrientationManager.stop();
        
//        updateRendering();
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused) {
        //mPaused = paused;
        //mOrientationManager.stop();
        //updateRendering();
    }

    /**
     * Start or stop rendering according to the timeline state.
     */
    private synchronized void updateRendering() {
        boolean shouldRender = (mHolder != null) && !mPaused;
        boolean rendering = mRenderThread != null;
        if (shouldRender != rendering) {
            if (shouldRender) {
                mRenderThread = new RenderThread();
                mRenderThread.start();
            } else {
                mRenderThread.quit();
                mRenderThread = null;
            }
        }
    }
    
    public void shutDown(){
    	if(mOrientationManager != null){
    		mOrientationManager.stop();
    		mOrientationManager = null;
    	}
    	if(mRenderThread != null){
    		mRenderThread.quit();
    		mRenderThread = null;
    	}
    }

    /**
     * Draws the view in the SurfaceHolder's canvas.
     */
    private synchronized void draw() {
        Canvas canvas;
        try {
            canvas = mHolder.lockCanvas();
        } catch (Exception e) {
            return;
        }
        if (canvas != null) {
        	mTouryView.draw(canvas);
            mHolder.unlockCanvasAndPost(canvas);
        }
    }

    /*
     * Getter for the marker list
     */
    public List<Marker> getMarkerList() {
		return mMarkerList;
	}

    /*
     * Sets the marker list
     * @param mMarkerList the marker list to be set
     * Side-effect: This method sorts the list according to its order property
     */
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

	/**
     * Redraws in the background.
     */
    private class RenderThread extends Thread {
        private boolean mShouldRun;

        /**
         * Initializes the background rendering thread.
         */
        public RenderThread() {
            mShouldRun = true;
        }

        /**
         * Returns true if the rendering thread should continue to run.
         *
         * @return true if the rendering thread should continue to run
         */
        private synchronized boolean shouldRun() {
            return mShouldRun;
        }

        /**
         * Requests that the rendering thread exit at the next opportunity.
         */
        public synchronized void quit() {
            mShouldRun = false;
            
        }

        @Override
        public void run() {
            while (shouldRun()) {
                draw();
                SystemClock.sleep(FRAME_TIME_MILLIS);
            }
        }
    }
}
