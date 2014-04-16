package twilight.of.the.devs.touryglass;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.google.android.gms.location.LocationClient;

import twilight.of.the.devs.mylibrary.Marker;
import twilight.of.the.devs.utils.DirectionUtils;
import twilight.of.the.devs.utils.MathUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.PorterDuff.Mode;
import android.location.Location;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.text.DynamicLayout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

public class TouryView extends View {

	private static final String TAG = TouryView.class.getName();
	private float mHeading;
	private Paint mPaint;
	private static final float DESCRIPTION_TEXT_HEIGHT = 64.0f;
	private static final float PLACE_TEXT_HEIGHT = 48.0f;
	private static final float PLACE_PIN_WIDTH = 14.0f;
    private static final float PLACE_TEXT_MARGIN = 8.0f;
    private static final int MAX_OVERLAPPING_PLACE_NAMES = 4;
	private TextPaint mTextPaint;
	private LiveCardRenderer mRenderer;
	private List<Marker> mMarkerList;
	private Location mCurrentLocation, otherLocation;
	private NumberFormat mNumberFormat;
	private Bitmap mPlaceBitmap;
	private DynamicLayout mDescLayout;
	private Marker mTriggeredMarker;
	private Paint mDescPaint;
	private float[] mDistance;
	private ArrayList<Rect> mAllBounds;
	private TouryService mService;
	private long mLastTime;
	private boolean hasReadAloud;
	private Integer searchResultId;
	private Context mContext;
	
	public LiveCardRenderer getRenderer() {
		return mRenderer;
	}

	public void setRenderer(LiveCardRenderer mRenderer) {
		this.mRenderer = mRenderer;
	}

	public TouryView(Context context) {
		super(context);
		setupUi(context);
	}
	
	public TouryView(Context context, AttributeSet attrs, int defStyles) {
		super(context, attrs, defStyles);
		setupUi(context);
	}

	public TouryView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupUi(context);
	}
	
	private void setupUi(Context context){
		mContext = context;
		mPaint = new Paint();
    	mPaint.setColor(Color.WHITE);
    	mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(PLACE_TEXT_HEIGHT);
        mPaint.setTypeface(Typeface.createFromFile(new File("/system/glass_fonts",
                "Roboto-Thin.ttf")));
        mDescPaint = new Paint();
    	mDescPaint.setColor(Color.WHITE);
    	mDescPaint.setStyle(Paint.Style.FILL);
        mDescPaint.setAntiAlias(true);
        mDescPaint.setTextSize(DESCRIPTION_TEXT_HEIGHT);
        mDescPaint.setTypeface(Typeface.createFromFile(new File("/system/glass_fonts",
                "Roboto-Thin.ttf")));

        mTextPaint = new TextPaint(mPaint);
//        mTextPaint2 = new TextPaint(mDescPaint);
//        mTextLayout = new LinkedList<DynamicLayout>();
//        mMarkerMap = new HashMap<DynamicLayout, Marker>();
        mNumberFormat = NumberFormat.getNumberInstance();
        mNumberFormat.setMaximumFractionDigits(2);
        mPlaceBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.place_mark);
        mTriggeredMarker = new Marker();
        mTriggeredMarker.setDescription("");
        mDistance = new float[]{0.0f};
        otherLocation = new Location("");
        mDescLayout = new DynamicLayout(mTriggeredMarker.getDescription(), mTextPaint, 640, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        setKeepScreenOn(true);
        mAllBounds = new ArrayList<Rect>();
        //searchResultId = 1;
	}
	
	public void setHeading(float head){
		mHeading = head;
		if(mHeading < 0) mHeading = 360 + mHeading;
	}
	
	public void setTriggeredMarker(Marker m){
		mTriggeredMarker = m;
		showTriggeredMarker();
	}
	
	public void removeTriggeredMarker(){
		mTriggeredMarker.setDescription("");
		mDescLayout = new DynamicLayout(mTriggeredMarker.getDescription(), new TextPaint(mDescPaint), 640, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
	}
	
	public void showTriggeredMarker(){
		mDescLayout = new DynamicLayout(mTriggeredMarker.getDescription(), new TextPaint(mDescPaint), 640, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
	}
	
	public void setMarkerList(List<Marker> list){
		if(list == null){ Log.d(TAG, "List is null"); return;}
		mMarkerList = list;
	}
	
	public void setSearchResultId(Integer id){
		this.searchResultId = id;
	}
	
	public void readDescription(String desc){
		long currentTime  = System.currentTimeMillis();
		//if(currentTime - mLastTime > 1000 * 5){ // wait 5 seconds to speak again
			if(mService.getTTS() != null)
				mService.getTTS().speak(mTriggeredMarker.getDescription(), TextToSpeech.QUEUE_FLUSH, null);
			hasReadAloud = true;
			//mMarkerList.remove(0);
			//mTriggeredMarker = null;
			//mLastTime = currentTime;
		//}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(0, Mode.CLEAR);
		
		if(mMarkerList == null || mMarkerList.isEmpty()){
			canvas.drawText("Welcome to Toury", 20, canvas.getHeight() / 2, mDescPaint);
		} else {
			
			if(mCurrentLocation != null && mTriggeredMarker != null){
				Location.distanceBetween(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), mTriggeredMarker.getMarkerLatitude(), mTriggeredMarker.getMarkerLongitude(), mDistance);	
			}
			
			/*
			 * Draw the description if looking in the direction of the marker.
			 */
			if(mTriggeredMarker != null && isLookingAtMarker(mDistance[0]) && !mTriggeredMarker.getDescription().isEmpty()){
				canvas.save();
				canvas.translate(20, 20);
				mDescLayout.draw(canvas);
				canvas.restore();
				if(!hasReadAloud)
					readDescription(mTriggeredMarker.getDescription());
				return;
			}
			mAllBounds.clear();

			for(Marker marker : mMarkerList){
				canvas.save();

				//Calculate bearing from my current location to the location of each marker.
				otherLocation.setLatitude(marker.getMarkerLatitude());
				otherLocation.setLongitude(marker.getMarkerLongitude());
				
				//Don't draw markers that are far away.
				Location.distanceBetween(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), marker.getMarkerLatitude(), marker.getMarkerLongitude(), mDistance);
				if(mDistance[0] > 1000) continue;
				
				//float bearing = (float) DirectionUtils.convertBearing(mCurrentLocation.bearingTo(otherLocation));
				float bearing = (float) DirectionUtils.getBearing(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), otherLocation.getLatitude(), otherLocation.getLongitude());
				
				// Measure the text and offset the text bounds to the location where the text
                // will finally be drawn.
                Rect textBounds = new Rect();
                mTextPaint.getTextBounds(marker.getTitle(), 0, marker.getTitle().length(), textBounds);
                
                if(searchResultId != null && marker.getId() == searchResultId){
                	mTextPaint.setARGB(255, 255, 0, 0);
                } else {
                	mTextPaint.setARGB(255, 255, 255, 255);
                }

                // Extend the bounds rectangle to include the pin icon and a small margin
                // to the right of the text, for the overlap calculations below.
                textBounds.left -= PLACE_PIN_WIDTH + PLACE_TEXT_MARGIN;
                textBounds.right += PLACE_TEXT_MARGIN;
                double diff = bearing - mHeading;
                if(diff < 0 && Math.abs(diff) > 180) bearing += 360;
//                Log.d(TAG, "Diff: " + diff);
                textBounds.offset((int)((((diff)) / 90) * canvas.getWidth()) + 15 + canvas.getWidth()/2, 0);
//                canvas.drawText("Bearing to " + marker.getTitle() + ": " + bearing, 20, 200, mPaint);
                if(mTriggeredMarker != null && isLookingAtMarker(mDistance[0]) && !mTriggeredMarker.getDescription().isEmpty())
                	textBounds.top = canvas.getHeight();
                else
                	textBounds.top = canvas.getHeight() - 32;
                textBounds.bottom = (int)(textBounds.top + PLACE_TEXT_HEIGHT);
                
                // This loop attempts to find the best vertical position for the string by
                // starting at the bottom of the display and checking to see if it overlaps
                // with any other labels that were already drawn. If there is an overlap, we
                // move up and check again, repeating this process until we find a vertical
                // position where there is no overlap, or when we reach the limit on
                // overlapping place names.
                boolean intersects;
                int numberOfTries = 0;
                do {
                    intersects = false;
                    numberOfTries++;
//                    textBounds.offset(0, (int) (textBounds.top - PLACE_TEXT_HEIGHT));
                    textBounds.top = (int) (textBounds.top - PLACE_TEXT_HEIGHT);
                    textBounds.bottom = (int)(textBounds.top + PLACE_TEXT_HEIGHT);
                    for (Rect existing : mAllBounds) {
                        if (Rect.intersects(existing, textBounds)) {
                            intersects = true;
                            break;
                        }
                    }
                    
                } while (intersects && numberOfTries <= MAX_OVERLAPPING_PLACE_NAMES);

                // Only draw the string if it would not go high enough to overlap the compass
                // directions. This means some places may not be drawn, even if they're nearby.
                if (numberOfTries <= MAX_OVERLAPPING_PLACE_NAMES) {
                    mAllBounds.add(textBounds);
                    if(mCurrentLocation != null){
                    	Location.distanceBetween(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), marker.getMarkerLatitude(), marker.getMarkerLongitude(), mDistance);
                    }
                    int alpha = (int)((10 / mDistance[0])*255);
    				alpha = alpha > 255 ? 255 : alpha;
    				mPaint.setAlpha(alpha);
    				diff = bearing - mHeading;
                    if(diff < 0 && Math.abs(diff) > 180) bearing += 360;
    				canvas.drawBitmap(mPlaceBitmap, (int)((((diff)) / 90)*canvas.getWidth()) - 15 + canvas.getWidth()/2, textBounds.top, mPaint);
    				canvas.drawText(marker.getTitle(), (int)((((diff)) / 90) * canvas.getWidth()) + 15 + canvas.getWidth()/2, textBounds.top + PLACE_TEXT_HEIGHT, mTextPaint);
                }
                if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("ordered", false)){
                	//Log.d(TAG, marker.toString());
                	break;
                }
			}
			//Log.d(TAG, "Broke out of loop");
		}
		//Debugging: Draws the current heading and location
		if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("debug", false))
		canvas.drawText("Heading: " + mHeading, 20, 256, mPaint);
//		if(mCurrentLocation!= null)
//			canvas.drawText("Loc: " + mNumberFormat.format(mCurrentLocation.getLatitude()) + ", " + mNumberFormat.format(mCurrentLocation.getLongitude()), 20, 300, mPaint);
	}
	
	public void setService(TouryService service){
		this.mService = service;
	}

	public Location getCurrentLocation() {
		return mCurrentLocation;
	}

	public void setCurrentLocation(Location mCurrentLocation) {
		this.mCurrentLocation = mCurrentLocation;
	}
	
	public boolean isWithinTenDegrees(int one, int two){
		return Math.abs(one - two) <= 10;
	}
	
	public boolean isLookingAtMarker(double distance){
		boolean result = false;
		otherLocation.setLatitude(mTriggeredMarker.getMarkerLatitude());
		otherLocation.setLongitude(mTriggeredMarker.getMarkerLongitude());
		float bearing = (float) DirectionUtils.convertBearing(mCurrentLocation.bearingTo(otherLocation));
//		Log.d(TAG, "Bearing: " + bearing + ", Heading: " + mHeading);
		result = Math.abs(bearing - mHeading) <= 20;//(10.0 / (100.0 / distance));
		return result;
	}
}
