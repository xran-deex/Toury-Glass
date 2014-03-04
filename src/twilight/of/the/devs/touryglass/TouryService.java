package twilight.of.the.devs.touryglass;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class TouryService extends Service {

	private static final String LIVE_CARD_TAG = "my_card";
	private static final String TAG = TouryService.class.getName();
	private LiveCard mLiveCard;
	
	private final IBinder mBinder = new LocalBinder();
	
	public class LocalBinder extends Binder {
        TouryService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TouryService.this;
        }
    }

	public TouryService() {
		super();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		publishCard(this);
		return START_STICKY;
	}
	
	private void publishCard(Context context) {
	    if (mLiveCard == null) {
	        TimelineManager tm = TimelineManager.from(context);
	        mLiveCard = tm.createLiveCard(LIVE_CARD_TAG);

	        // Enable direct rendering.
	        mLiveCard.setDirectRenderingEnabled(true);
	        mLiveCard.getSurfaceHolder().addCallback(new LiveCardRenderer(this));

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
		super.onDestroy();
	}
}
