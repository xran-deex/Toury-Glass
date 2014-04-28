package twilight.of.the.devs.touryglass;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import twilight.of.the.devs.mylibrary.Marker;
import twilight.of.the.devs.utils.DirectionUtils;
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
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class TouryView extends View {
	
	/*
	 * Note: Much of the following code is adapted from the Compass Example App
	 * 
	 */

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
	private boolean hasReadAloud;
	private Integer searchResultId;
	private Context mContext;
	private boolean mShowTitle;
	private boolean dimMarkers;

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
	
	/*
	 * Sets up the initial user interface
	 */
	private void setupUi(Context context){
		mContext = context;
		mPaint = new Paint();
    	mPaint.setColor(Color.WHITE);
    	mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(PLACE_TEXT_HEIGHT);

        mPaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        mDescPaint = new Paint();
    	mDescPaint.setColor(Color.WHITE);
    	mDescPaint.setStyle(Paint.Style.FILL);
        mDescPaint.setAntiAlias(true);
        mDescPaint.setTextSize(DESCRIPTION_TEXT_HEIGHT);

        mDescPaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));

        mTextPaint = new TextPaint(mPaint);

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
        mShowTitle = true;
	}
	
	/*
	 * Get the LiveCardRender
	 */
	public LiveCardRenderer getRenderer() {
		return mRenderer;
	}

	/*
	 * Set the LiveCardRender
	 */
	public void setRenderer(LiveCardRenderer mRenderer) {
		this.mRenderer = mRenderer;
	}
	
	/*
	 * Set the current heading
	 */
	public void setHeading(float head){
		mHeading = head;
		if(mHeading < 0) mHeading = 360 + mHeading;
	}
	
	/*
	 * Sets the triggered marker.
	 * Updates the text size, removes the title.
	 */
	public void setTriggeredMarker(Marker m){
		mTriggeredMarker = m;
		hasReadAloud = false;
		mDescPaint.setTextSize(DESCRIPTION_TEXT_HEIGHT);
		mShowTitle = false;
		showTriggeredMarker();
	}
	
	/*
	 * Remove the triggered marker.
	 */
	public void removeTriggeredMarker(){
		mTriggeredMarker.setDescription("");
		mShowTitle = true;
		mDescPaint.setTextSize(28);
		mDescLayout = new DynamicLayout(mTriggeredMarker.getDescription(), new TextPaint(mDescPaint), 640, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
	}
	
	/*
	 * Display the triggered marker
	 */
	public void showTriggeredMarker(){
		
		mDescLayout = new DynamicLayout(mTriggeredMarker.getDescription(), new TextPaint(mDescPaint), 640, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
	}
	
	/*
	 * Set the marker list.
	 */
	public void setMarkerList(List<Marker> list){
		if(list == null){ Log.d(TAG, "List is null"); return;}
		mMarkerList = list;
	}
	
	/*
	 * Set the id of a marker to be displayed red.
	 */
	public void setSearchResultId(Integer id){
		this.searchResultId = id;
	}
	
	/*
	 * Reads a description using text to speech.
	 */
	public void readDescription(String desc){
		if(mService.getTTS() != null)
			mService.getTTS().speak(mTriggeredMarker.getDescription(), TextToSpeech.QUEUE_FLUSH, null);
		//Only read once...
		hasReadAloud = true;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(0, Mode.CLEAR);
		
		//Display distances if the preference is set.
		boolean show_distances = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("show_distances", false);
		
		//If there are no markers to show, display the Welcome text.
		if(mMarkerList == null || mMarkerList.isEmpty() || mCurrentLocation == null){
			canvas.drawText("Welcome to Toury", 20, canvas.getHeight() / 2, mDescPaint);
		} else {
			if(mShowTitle)
				canvas.drawText("Toury", canvas.getWidth()/2 - 15, 20, mDescPaint);
			if(mCurrentLocation != null && mTriggeredMarker != null){
				Location.distanceBetween(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), mTriggeredMarker.getMarkerLatitude(), mTriggeredMarker.getMarkerLongitude(), mDistance);	
			}
			
			/*
			 * Draw the description if looking in the direction of the marker.
			 */
			if(mTriggeredMarker != null && isLookingAtMarker(mDistance[0]) && !mTriggeredMarker.getDescription().isEmpty()){
				canvas.save();
				canvas.translate(20, 20);
				mDescPaint.setTextSize(DESCRIPTION_TEXT_HEIGHT);
				mDescLayout.draw(canvas);
				canvas.restore();
				if(!hasReadAloud)
					readDescription(mTriggeredMarker.getDescription());
				dimMarkers = true;
			} else {
				dimMarkers = false;
				mDescPaint.setTextSize(28);
				canvas.drawText("Toury", canvas.getWidth()/2 - 20, 20, mDescPaint);
			}
			mAllBounds.clear();

			//Draw all of the markers...
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
                
                textBounds.offset(getOffset(diff, canvas.getWidth()) + 15 + canvas.getWidth()/2, 0);
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
                    int alpha = (int)((50 / mDistance[0])*255);
    				alpha = alpha > 255 ? 255 : alpha;
    				
    				//Makes the markers faint while a description is shown.
    				if(dimMarkers)
    					alpha = 30;
//    				mPaint.setAlpha(alpha);
//    				mTextPaint.setAlpha(alpha);
    				diff = bearing - mHeading;

    				//Attempt to display markers around degree 0/360
    				for(int i = -1; i <= 1; i+=1){
    					diff += i * 360;
    					canvas.drawBitmap(mPlaceBitmap, getOffset(diff, canvas.getWidth()) - 15 + canvas.getWidth()/2, textBounds.top, mPaint);
    					if(show_distances)
    						canvas.drawText(marker.getTitle()+" (" + (int)mDistance[0] +"m)", getOffset(diff, canvas.getWidth()) + 15 + canvas.getWidth()/2, textBounds.top + PLACE_TEXT_HEIGHT, mTextPaint);
    					else
    						canvas.drawText(marker.getTitle(), getOffset(diff, canvas.getWidth()) + 15 + canvas.getWidth()/2, textBounds.top + PLACE_TEXT_HEIGHT, mTextPaint);
    				}
                }
                if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("ordered", false)){
                	break;
                }
			}
		}
		//Debugging: Draws the current heading and location
		if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("debug", false))
			canvas.drawText("Heading: " + mHeading, 20, 256, mPaint);
	}
	
	/*
	 * Sets the TouryService for local access.
	 */
	public void setService(TouryService service){
		this.mService = service;
	}

	/*
	 * Retrieve the current location
	 * @return Location the user's current location
	 */
	public Location getCurrentLocation() {
		return mCurrentLocation;
	}

	/*
	 * Set the current location
	 * @param mCurrentLocation the user's current location
	 */
	public void setCurrentLocation(Location mCurrentLocation) {
		this.mCurrentLocation = mCurrentLocation;
	}
	
	/*
	 * Check if the 2 headings are within 10 degrees of each other.
	 * @param headingOne the first heading
	 * @param headingTwo the second heading
	 * @return true if headingOne is within 10 degrees of headingTwo
	 */
	public boolean isWithinTenDegrees(int headingOne, int headingTwo){
		return Math.abs(headingOne - headingTwo) <= 10;
	}
	
	/*
	 * Used to correctly display a marker based on the current heading.
	 * @param bearing the bearing to a marker from the user's current location
	 * @param width the width of the display area
	 * @return int the correct coordinate for displaying on the screen.
	 */
	private int getOffset(double bearing, float width){
		return (int)(((bearing) / 90) * width);
	}
	
	/*
	 * Determine if a user if looking at a marker.
	 * @param distance the distance to the marker
	 * @return true if the user's heading is within 20 degrees of the marker's bearing
	 */
	public boolean isLookingAtMarker(double distance){
		if(mTriggeredMarker == null || mCurrentLocation == null) return false;
		boolean result = false;
		otherLocation.setLatitude(mTriggeredMarker.getMarkerLatitude());
		otherLocation.setLongitude(mTriggeredMarker.getMarkerLongitude());
		float bearing = (float) DirectionUtils.convertBearing(mCurrentLocation.bearingTo(otherLocation));
		result = Math.abs(bearing - mHeading) <= 20;
		return result;
	}
}
