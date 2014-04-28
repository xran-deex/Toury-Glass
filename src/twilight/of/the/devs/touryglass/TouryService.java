package twilight.of.the.devs.touryglass;

import java.util.LinkedList;
import java.util.List;

import twilight.of.the.devs.mylibrary.Marker;
import twilight.of.the.devs.touryglass.provider.TouryProvider.TouryProviderMetaData;
import twilight.of.the.devs.touryglass.provider.TouryProvider.TouryProviderMetaData.MarkersTableMetaData;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.gms.location.Geofence;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class TouryService extends Service {

	private static final String LIVE_CARD_TAG = "my_card";
	private static final String TAG = TouryService.class.getName();
	private LiveCard mLiveCard;
	private boolean showTourList = false;
	private LinkedList<Marker> mCurrentMarkers;
	private LinkedList<Geofence> mGeofences;

	private final IBinder mBinder = new LocalBinder();
	private TextToSpeech mSpeech;
	private LiveCardRenderer mLiveCardRenderer;
	
	/*
	 * Bind to the TouryService
	 */
	public class LocalBinder extends Binder {
        TouryService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TouryService.this;
        }
    }

	public TouryService() {
		super();
	}
	
	/*
	 * Access the LiveCardRenders
	 */
	public LiveCardRenderer getRenderer(){
		return mLiveCardRenderer;
	}
	
	/*
	 * Tour list setter
	 * @param showTourList 
	 */
	public void showTourList(boolean showTourList){
		this.showTourList = showTourList;
	}
	
	/*
	 * Tour list getter
	 * @return true if the tour list should be shown
	 */
	public boolean showTourList(){
		return showTourList;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	/*
	 * Accessor for the text to speech engine
	 * @return TextToSpeech the current TTS engine
	 */
	public TextToSpeech getTTS(){
		return mSpeech;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		publishCard(this);
		Intent syncService = new Intent(this, SyncService.class);
		startService(syncService);
		int tourId = PreferenceManager.getDefaultSharedPreferences(this).getInt("last_tour", -1);
		if(tourId != -1){
			loadTour(tourId);
		}
		mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Do nothing.
            }
        });
		return START_STICKY;
	}
	
	/*
	 * Publish the live card
	 */
	private void publishCard(Context context) {
	    if (mLiveCard == null) {
	    	mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
	        // Enable direct rendering.
	        mLiveCardRenderer = new LiveCardRenderer(this);
	        mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mLiveCardRenderer);

	        Intent intent = new Intent(context, MainActivity.class);
	        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
	        mLiveCard.setAction(PendingIntent.getActivity(context, 0,
	                intent, 0));
	        mLiveCard.publish(LiveCard.PublishMode.REVEAL);
	    } else {
	        // Card is already published.
	    	mLiveCard.navigate();
	        return;
	    }
	}
	
	@Override
	public void onDestroy() {
		if (mLiveCard != null) {
	        mLiveCard.unpublish();
	        mLiveCard = null;
	    }
		if(mSpeech != null){
			mSpeech.shutdown();
			mSpeech = null;
		}
		if(mLiveCardRenderer != null){
			Log.d(TAG, "Killing renderer...");
			
			mLiveCardRenderer = null;
		}
		Log.d(TAG, "Service onDestroy");
		super.onDestroy();
	}
	
	/*
	 * Access the geofence list
	 * @return LinkedList<Geofence> the current list of geofences
	 */
	public LinkedList<Geofence> getGeofences() {
		return mGeofences;
	}

	/*
	 * Setter for the geofence list
	 * @param mGeofences the list of geofences to be set
	 */
	public void setGeofences(LinkedList<Geofence> mGeofences) {
		this.mGeofences = mGeofences;
	}
	
	/*
	 * Access the marker list
	 * @return LinkedList<Marker> the current list of markers
	 */
	public LinkedList<Marker> getCurrentMarkers() {
		return mCurrentMarkers;
	}

	/*
	 * Setter for the marker list
	 * @param mCurrentMarkers the list of markers to set
	 */
	public void setCurrentMarkers(LinkedList<Marker> mCurrentMarkers) {
		this.mCurrentMarkers = mCurrentMarkers;
	}
	
	/*
	 * Loads the markers of a given tour into memory
	 * @param tourId the id of the selected tour
	 */
	public void loadTour(final int tourId){
    	
    	new AsyncTask<Void, Void, List<Geofence>>(){

			
			@Override
			protected List<Geofence> doInBackground(Void... params) {
					
				mGeofences = new LinkedList<Geofence>();
				mCurrentMarkers = new LinkedList<Marker>();
				Uri uri = Uri.withAppendedPath(TouryProviderMetaData.ToursTableMetaData.CONTENT_URI, "tour/" + tourId + "/markers");
				Cursor c = getContentResolver().query(uri, null, null, null, null);

				while(c.moveToNext()){
					Marker g = new Marker();
							g.setId((int)c.getLong(c.getColumnIndex(MarkersTableMetaData._ID)));
							g.setTriggerLatitude(c.getDouble(c.getColumnIndex(MarkersTableMetaData.TRIGGER_LATITUDE)));
							g.setTriggerLongitude(c.getDouble(c.getColumnIndex(MarkersTableMetaData.TRIGGER_LONGITUDE)));
							g.setMarkerLatitude(c.getDouble(c.getColumnIndex(MarkersTableMetaData.MARKER_LATITUDE)));
							g.setMarkerLongitude(c.getDouble(c.getColumnIndex(MarkersTableMetaData.MARKER_LONGITUDE)));
							g.setRadius(c.getDouble(c.getColumnIndex(MarkersTableMetaData.RADIUS)));
							g.setDescription(c.getString(c.getColumnIndex(MarkersTableMetaData.DESCRIPTION)));
							g.setDirection(c.getDouble(c.getColumnIndex(MarkersTableMetaData.DIRECTION)));
							g.setOrder(c.getInt(c.getColumnIndex(MarkersTableMetaData.ORDER)));
							Log.d(TAG, "Direction: " + g.getDirection());
							g.setTitle(c.getString(c.getColumnIndex(MarkersTableMetaData.TITLE)));
                	mCurrentMarkers.add(g);
                	mGeofences.add(g.toGeofence());
				}

                mLiveCardRenderer.setMarkerList(mCurrentMarkers);
        		
				return null;
			}
    		
    	}.execute();
    }
}
