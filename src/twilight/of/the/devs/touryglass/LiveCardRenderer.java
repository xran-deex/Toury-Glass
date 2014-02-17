package twilight.of.the.devs.touryglass;

import twilight.of.the.devs.utils.OrientationManager;
import twilight.of.the.devs.utils.OrientationManager.OnChangedListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.glass.timeline.DirectRenderingCallback;

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
	

	private Context mContext;
	
	public LiveCardRenderer(Context context) {
		this.mContext = context;
		LayoutInflater inflater = LayoutInflater.from(mContext);
		FrameLayout layout = (FrameLayout)inflater.inflate(R.layout.activity_main, null);
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
        mOrientationManager.stop();
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
