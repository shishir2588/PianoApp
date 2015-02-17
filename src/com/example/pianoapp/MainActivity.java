package com.example.pianoapp;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class MainActivity extends Activity implements InstrumentSelectionListener {
	
	private static final String TAG = "PianoAppMain";
	
	private static final int NUM_KEYS = 17;
	
	private final int[] mKeyIds = {R.id.key_c4, R.id.key_csharp4, R.id.key_d4, R.id.key_eflat4,
			R.id.key_e4, R.id.key_f4, R.id.key_fsharp4, R.id.key_g4, R.id.key_gsharp4,
			R.id.key_a4, R.id.key_bflat4, R.id.key_b4, R.id.key_c5, R.id.key_csharp5,
			R.id.key_d5, R.id.key_eflat5, R.id.key_e5
	};
	
	private int[] mNoteResourceIds;
	TypedArray mArr;
	
	public enum Instrument
	{
		PIANO,
		GUITAR,
		FLUTE,
		HARMONICA,
		VIOLIN,
		TRUMPET,
		SITAR,
		ACOUSTIC_BASS
	}
	
	public static final String KEY_CURR_INSTRUMENT = "CurrentInstrument";
	
	private Instrument mCurrInstru ;
	
	private Map<Integer, Integer> mKeyMidiMap = null;
	private SoundPool mSoundPool;
	
	
	// rectangle that holds bounds of the current touched key
	private Rect mRect;
	// rectangles to hold bounds of next and previous overlapping black keys
	// these are used to detect when the pointer moves from white to black key
	private Rect mNextBlackKeyRect;
	private Rect mPrevBlackKeyRect;
	
	private RelativeLayout mParent;
	
	private final View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			switch(event.getAction())
			{
			case MotionEvent.ACTION_DOWN:
				Log.i(TAG, "Action down at ("+event.getX()+","+event.getY()+")");
				mSoundPool.play(mKeyMidiMap.get(v.getId()).intValue(), 1.0f, 1.0f, 1, 0, 1.0f);
				if(isWhiteKey(v))
				{
					((ImageView)v).setImageDrawable(getResources().getDrawable(R.drawable.piano_white_key_pressed));
				}
				else
				{
					((ImageView)v).setImageDrawable(getResources().getDrawable(R.drawable.piano_black_key_pressed));
				}
				
				// get area occupied by current key
				mRect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
				
				// if white key, get overlapping black keys' areas
				if(isWhiteKey(v))
				{
					int idBlackKeyHigh = getOverlappingBlackKeyHigher(v);
					if(idBlackKeyHigh != 0)
					{
						View tmp = findViewById(idBlackKeyHigh);
						mNextBlackKeyRect = new Rect(tmp.getLeft(), tmp.getTop(), tmp.getRight(), tmp.getBottom());
					}
					else
					{
						mNextBlackKeyRect = null;
					}
					
					int idBlackKeyLow = getOverlappingBlackKeyLower(v);
					if(idBlackKeyLow != 0)
					{
						View tmp = findViewById(idBlackKeyLow);
						mPrevBlackKeyRect = new Rect(tmp.getLeft(), tmp.getTop(), tmp.getRight(), tmp.getBottom());
					}
					else
					{
						mPrevBlackKeyRect = null;
					}
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if(isWhiteKey(v))
				{
					((ImageView)v).setImageDrawable(getResources().getDrawable(R.drawable.piano_white_key_unpressed));
				}
				else
				{
					((ImageView)v).setImageDrawable(getResources().getDrawable(R.drawable.piano_black_key_unpressed));
				}
				break;
			case MotionEvent.ACTION_MOVE:
				// check if pointer has moved outside key
				if( (!mRect.contains(v.getLeft()+(int)event.getX(), v.getTop()+(int)event.getY())) ||
						(isWhiteKey(v) && (mNextBlackKeyRect!=null) && (mNextBlackKeyRect.contains(v.getLeft()+(int)event.getX(), v.getTop()+(int)event.getY()))) ||
						(isWhiteKey(v) && (mPrevBlackKeyRect!=null) && (mPrevBlackKeyRect.contains(v.getLeft()+(int)event.getX(), v.getTop()+(int)event.getY()))))
				{
					// pointer has moved outside key, send artificial ACTION_CANCEL event on current key
					MotionEvent upEvent = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 
							MotionEvent.ACTION_CANCEL, event.getX(), event.getY(), 0);
					v.dispatchTouchEvent(upEvent);
					upEvent.recycle();
					
					// send artificial ACTION_DOWN event to new key
					MotionEvent downEvent = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
							MotionEvent.ACTION_DOWN, v.getLeft()+event.getX(), v.getTop()+event.getY(), 0);
					mParent.dispatchTouchEvent(downEvent);
					downEvent.recycle();
				}
			}
				
			return true;
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mParent = (RelativeLayout)findViewById(R.id.parent_view);
		
		// get notes' raw resource IDs for current instrument (default: piano)
		mCurrInstru = Instrument.PIANO;
		mNoteResourceIds = new int[NUM_KEYS];
		mArr = getResources().obtainTypedArray(R.array.piano_notes);
		getNoteRawResources();
		
		ImageView imageView = null;
		for(int i=0; i<NUM_KEYS; i++)
		{
			imageView = (ImageView)findViewById(mKeyIds[i]);
			imageView.setOnTouchListener(mOnTouchListener);
		}
		if(mKeyMidiMap == null)
		{
			loadMIDISounds();
		}
	}
	
	private void getNoteRawResources()
	{
		switch(mCurrInstru)
		{
		case PIANO:
			mArr = getResources().obtainTypedArray(R.array.piano_notes);
			break;
		case GUITAR:
			mArr = getResources().obtainTypedArray(R.array.guitar_notes);
			break;
		case FLUTE:
			mArr = getResources().obtainTypedArray(R.array.flute_notes);
			break;
		case HARMONICA:
			mArr = getResources().obtainTypedArray(R.array.harmonica_notes);
			break;
		case VIOLIN:
			mArr = getResources().obtainTypedArray(R.array.violin_notes);
			break;
		case SITAR:
			mArr = getResources().obtainTypedArray(R.array.sitar_notes);
			break;
		case ACOUSTIC_BASS:
			mArr = getResources().obtainTypedArray(R.array.acoustic_bass_notes);
			break;
		case TRUMPET:
			mArr = getResources().obtainTypedArray(R.array.trumpet_notes);
			break;
		}
		for(int i=0; i<NUM_KEYS; i++)
		{
			mNoteResourceIds[i] = mArr.getResourceId(i, 0);
		}
		mArr.recycle();
	}
	
	@SuppressLint("UseSparseArrays")
	private void loadMIDISounds()
	{
		mSoundPool = new SoundPool(NUM_KEYS, AudioManager.STREAM_MUSIC, 0);
		
		mKeyMidiMap = new HashMap<Integer, Integer>(NUM_KEYS);
		for(int i=0; i<NUM_KEYS; i++)
		{
			mKeyMidiMap.put(mKeyIds[i], mSoundPool.load(this, mNoteResourceIds[i], 0));
		}
	}
	
	private boolean isWhiteKey(View key)
	{
		switch (key.getId())
		{
		case R.id.key_c4:
		case R.id.key_d4:
		case R.id.key_e4:
		case R.id.key_f4:
		case R.id.key_g4:
		case R.id.key_a4:
		case R.id.key_b4:
		case R.id.key_c5:
		case R.id.key_d5:
		case R.id.key_e5:
			return true;
		default:
			return false;
				
		}
	}
	
	private int getOverlappingBlackKeyHigher(View key)
	{
		switch(key.getId())
		{
		case R.id.key_c4:
			return R.id.key_csharp4;
		case R.id.key_d4:
			return R.id.key_eflat4;
		case R.id.key_f4:
			return R.id.key_fsharp4;
		case R.id.key_g4:
			return R.id.key_gsharp4;
		case R.id.key_a4:
			return R.id.key_bflat4;
		case R.id.key_c5:
			return R.id.key_csharp5;
		case R.id.key_d5:
			return R.id.key_eflat5;
		default:
			return 0;
		}
	}
	
	private int getOverlappingBlackKeyLower(View key)
	{
		switch(key.getId())
		{
		case R.id.key_d4:
			return R.id.key_csharp4;
		case R.id.key_e4:
			return R.id.key_eflat4;
		case R.id.key_g4:
			return R.id.key_fsharp4;
		case R.id.key_a4:
			return R.id.key_gsharp4;
		case R.id.key_b4:
			return R.id.key_bflat4;
		case R.id.key_d5:
			return R.id.key_csharp5;
		case R.id.key_e5:
			return R.id.key_eflat5;
		default:
			return 0;
		}
	}
	
	// implement InstrumentSelectionListener
	@Override
	public void onInstrumentSelect(int instruIdx)
	{
		mCurrInstru = Instrument.values()[instruIdx];
		getNoteRawResources();
		mSoundPool.release();
		mSoundPool = null;
		mKeyMidiMap = null;
		loadMIDISounds();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.select_instru) {
			FragmentManager fm = getFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			Fragment prev = fm.findFragmentByTag("InstrumentPicker");
			if(prev != null)
			{
				ft.remove(prev);
			}
			ft.addToBackStack(null);
			Bundle args = new Bundle();
			args.putInt(KEY_CURR_INSTRUMENT, mCurrInstru.ordinal());
			InstrumentSelectFragment instruFragment = new InstrumentSelectFragment();
			instruFragment.setArguments(args);
			instruFragment.show(ft, "InstrumentPicker");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
