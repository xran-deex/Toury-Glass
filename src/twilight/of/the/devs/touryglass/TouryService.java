package twilight.of.the.devs.touryglass;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONArray;
import org.json.JSONObject;

import twilight.of.the.devs.mylibrary.MARKER_STATE;
import twilight.of.the.devs.mylibrary.Marker;
import twilight.of.the.devs.touryglass.provider.TouryProvider.TouryProviderMetaData;
import twilight.of.the.devs.touryglass.provider.TouryProvider.TouryProviderMetaData.MarkersTableMetaData;
import twilight.of.the.devs.utils.GeofenceManager;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

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
import android.util.Base64;
import android.util.Log;

public class TouryService extends Service {

	private static final String LIVE_CARD_TAG = "my_card";
	private static final String TAG = TouryService.class.getName();
	private LiveCard mLiveCard;
	private boolean showTourList = false;
	private LinkedList<Geofence> mGeofences;
	public LinkedList<Geofence> getGeofences() {
		return mGeofences;
	}

	public void setGeofences(LinkedList<Geofence> mGeofences) {
		this.mGeofences = mGeofences;
	}

	private LinkedList<Marker> mCurrentMarkers;
	
	public LinkedList<Marker> getCurrentMarkers() {
		return mCurrentMarkers;
	}

	public void setCurrentMarkers(LinkedList<Marker> mCurrentMarkers) {
		this.mCurrentMarkers = mCurrentMarkers;
	}

	private final IBinder mBinder = new LocalBinder();
	private TextToSpeech mSpeech;
	private LiveCardRenderer mLiveCardRenderer;
	
	public class LocalBinder extends Binder {
        TouryService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TouryService.this;
        }
    }

	public TouryService() {
		super();
	}
	
	public LiveCardRenderer getRenderer(){
		return mLiveCardRenderer;
	}
	
	public void showTourList(boolean yes){
		showTourList = yes;
	}
	
	public boolean showTourList(){
		return showTourList;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
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
		return START_NOT_STICKY;
	}
	
	private void publishCard(Context context) {
	    if (mLiveCard == null) {
	        TimelineManager tm = TimelineManager.from(context);
	        mLiveCard = tm.createLiveCard(LIVE_CARD_TAG);

	        // Enable direct rendering.
	        mLiveCard.setDirectRenderingEnabled(true);
	        mLiveCardRenderer = new LiveCardRenderer(this);
	        mLiveCard.getSurfaceHolder().addCallback(mLiveCardRenderer);

	        Intent intent = new Intent(context, MainActivity.class);
	        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
	        mLiveCard.setAction(PendingIntent.getActivity(context, 0,
	                intent, 0));
	        mLiveCard.publish(LiveCard.PublishMode.REVEAL);
	    } else {
	        // Card is already published.
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
		
		super.onDestroy();
	}
	
	public void loadTour(final int tourId){
    	
//    	LocationFragment.tour_id = tourId;
    	new AsyncTask<Void, Void, List<Geofence>>(){

			
			@Override
			protected List<Geofence> doInBackground(Void... params) {
				
//				if(loadFromREST){
//					HttpClient client = new DefaultHttpClient();
//	                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
//	                HttpResponse response;
//	
//	                try {
//	                    HttpGet post = new HttpGet("http://valis.strangled.net:7000/api/markers/");
//	                    String authorizationString = "Basic " + Base64.encodeToString(
//	    				        ("randy" + ":" + "greenday").getBytes(),
//	    				        Base64.NO_WRAP); 
//	                    
//	                    
//	                    post.addHeader("Authorization", authorizationString);
//	                    response = client.execute(post);
//	                   
//	                    
//	
//	                    /*Checking response */
//	                    if(response!=null){
//	                        InputStream in = response.getEntity().getContent(); //Get the data in the entity
//	                        String res = new DataInputStream(in).readLine();
//	                        Log.d(TAG, res);
//	                        JSONObject obj = new JSONObject(res);
//	                        JSONArray results = obj.getJSONArray("results");
//	                    
//		                    for(int i = 1; i <= results.length(); i++){
//		                    	JSONObject j = results.getJSONObject(i-1);
//		                    	Marker g = new Marker();
//		                    	g.setId(j.getInt("id"));
//		                    	g.setDirection(j.getDouble("direction"));
//		                    	g.setDescription(j.getString("description"));
//		                    	g.setTriggerLatitude(j.getDouble("trigger_latitude"));
//		                    	g.setTriggerLongitude(j.getDouble("trigger_longitude"));
//		                    	g.setMarkerLatitude(j.getDouble("marker_latitude"));
//		                    	g.setMarkerLongitude(j.getDouble("marker_longitude"));
//		                    	g.setRadius(j.getDouble("radius"));
//		                    	g.setTitle(j.getString("title"));
//		                    	g.setTourId(j.getInt("tour"));
//		                    	mGeofences.add(g.toGeofence());
//		                    	mCurrentMarkers.add(g);
//		                    }
//	                    }
//	                } catch(Exception e) {
//	                  e.printStackTrace();
//	                }
//				} else {
				
					
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
//				}
                
//                if(!mCurrentMarkers.isEmpty()) {
//                	addGeofences();
//                	new ConnectThread(mBluetoothAdapter, mConnectedDevice, MARKER_STATE.INITIALIZE_LIST).execute((Marker[]) mCurrentMarkers.toArray(new Marker[]{}));
//                }
//                else {
//                	mRequestType = REQUEST_TYPE.CONNECT;
//                	mLocationClient = new LocationClient(MainActivity.this, MainActivity.this, MainActivity.this);
//                	mLocationClient.connect();
//                }
                mLiveCardRenderer.setMarkerList(mCurrentMarkers);
        		
				return null;
			}
    		
    	}.execute();
    }
}
