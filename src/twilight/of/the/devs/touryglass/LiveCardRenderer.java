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

import twilight.of.the.devs.utils.OrientationManager;
import twilight.of.the.devs.utils.OrientationManager.OnChangedListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.glass.timeline.DirectRenderingCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import twilight.of.the.devs.mylibrary.*;

public class LiveCardRenderer implements DirectRenderingCallback {

    // About 30 FPS.
    private static final long FRAME_TIME_MILLIS = 33;

	protected static final String TAG = LiveCardRenderer.class.getName();

    private SurfaceHolder mHolder;
    private boolean mPaused;
    private RenderThread mRenderThread;
    private float mHeading;
	private OrientationManager mOrientationManager;
	//private Layout layout;
	private HeadingView mHeadingView;
	private SimpleGeofence mGeofence;
	
	private Context mContext;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, final Intent intent) {
			Log.d(TAG, "Received location");
			mGeofence = ((SimpleGeofence)intent.getSerializableExtra("loc"));
			//Log.d(TAG, geofence.toString());
			//mHeadingView.setGeofence(geofence.getDescription());
		    	
//		    	new AsyncTask<Void, Void, SimpleGeofence>(){
//
//					@Override
//					protected SimpleGeofence doInBackground(Void... params) {
//						List<Geofence> result = new LinkedList<Geofence>();
//						HttpClient client = new DefaultHttpClient();
//		                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
//		                HttpResponse response;
//		                String path = intent.getStringExtra("loc");
//		                String[] parts = path.split("/");
//		                SimpleGeofence g = null;
//		                try {
//		                    HttpGet post = new HttpGet("http://valis.strangled.net:9000/api/tours/" + parts[parts.length-1]);
//		                    String authorizationString = "Basic " + Base64.encodeToString(
//		    				        ("randy" + ":" + "greenday").getBytes(),
//		    				        Base64.NO_WRAP); 
//		                    
//		                    
//		                    post.addHeader("Authorization", authorizationString);
//		                    response = client.execute(post);
//		                    
//		                    
//
//		                    /*Checking response */
//		                    if(response!=null){
//		                        InputStream in = response.getEntity().getContent(); //Get the data in the entity
//		                        String res = new DataInputStream(in).readLine();
//		                        JSONObject obj = new JSONObject(res);
//		                        g = new SimpleGeofence(obj.getString("url"), obj.getDouble("latitude"), obj.getDouble("longitude"), (float) obj.getDouble("radius"), 100000000L, Geofence.GEOFENCE_TRANSITION_ENTER|Geofence.GEOFENCE_TRANSITION_EXIT|Geofence.GEOFENCE_TRANSITION_DWELL);
//
//		                        g.setDescription(obj.getString("description"));
//		                    }
//
//		                } catch(Exception e) {
//		                    e.printStackTrace();
//		                }
//		                
//		                	
//						return g;
//					}
//					
//					@Override
//					protected void onPostExecute(
//							twilight.of.the.devs.touryglass.SimpleGeofence result) {
//						mHeadingView.setGeofence(result.getDescription());
//						super.onPostExecute(result);
//					}
//		    		
//		    	}.execute();
		}
	};

	private ServerThread mServerThread;
	
	public LiveCardRenderer(Context context) {
		this.mContext = context;
		LayoutInflater inflater = LayoutInflater.from(mContext);
		FrameLayout layout = (FrameLayout)inflater.inflate(R.layout.activity_main, null);
		mServerThread = new ServerThread(context);
		mServerThread.start();
		LocalBroadcastManager.getInstance(context).registerReceiver(mReceiver, new IntentFilter("location"));
		mHeadingView = (HeadingView)layout.findViewById(R.id.heading);
		SensorManager sensorManager =
                (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        LocationManager locationManager =
                (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
    	mOrientationManager = new OrientationManager(sensorManager, locationManager);
    	mOrientationManager.addOnChangedListener(new OnChangedListener() {
			
			@Override
			public void onOrientationChanged(OrientationManager orientationManager) {
				mHeading = mOrientationManager.getHeading();
				mHeadingView.setHeading(mHeading);
				
				if(mGeofence != null && isWithinTenDegrees((int)mHeading, (int)mGeofence.getDirection()))
					mHeadingView.setGeofence(mGeofence.getDescription());
				else if (mGeofence != null){
					mHeadingView.setGeofence("");
				}
			}
			
			@Override
			public void onLocationChanged(OrientationManager orientationManager) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAccuracyChanged(OrientationManager orientationManager) {
				// TODO Auto-generated method stub
				
			}
		});
    	mOrientationManager.start();
	}
	
	public boolean isWithinTenDegrees(int one, int two){
		return Math.abs(one - two) <= 10;
	}

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Update your views accordingly.
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

        mServerThread.closeSocket();
        mRenderThread.quit();
        mRenderThread = null;
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
        	mHeadingView.draw(canvas);
            mHolder.unlockCanvasAndPost(canvas);
        }
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
