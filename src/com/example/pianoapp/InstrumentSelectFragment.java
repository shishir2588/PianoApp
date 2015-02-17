package com.example.pianoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

public class InstrumentSelectFragment extends DialogFragment {
	
	private static final String TAG = "InstrumentSelectDialog";
	private final String[] instruments = {"Piano", "Guitar", "Flute", "Harmonica", "Violin",
			"Trumpet", "Sitar", "Acoustic Bass"
	};
	private InstrumentSelectionListener mListener;
	private int mSelected;
	private int mPrevSelection;
	
	@Override
	public void onAttach(Activity a)
	{
		super.onAttach(a);
		try
		{
			mListener = (InstrumentSelectionListener)a;
		}
		catch(ClassCastException e)
		{
			Log.e(TAG, "Activity must implement InstrumentSelectionListener");
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		Bundle args = getArguments();
		mSelected = args.getInt(MainActivity.KEY_CURR_INSTRUMENT, -1);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
							.setTitle("Select Instrument")
							.setSingleChoiceItems(instruments, mSelected, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									mPrevSelection = mSelected;
									mSelected = which;
									Log.i(TAG, "Selected"+MainActivity.Instrument.values()[mSelected]);
								}
							})
							.setPositiveButton("OK", null)
							.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									mSelected = mPrevSelection;
								}
							});
		return builder.create();
	}
	
	@Override
	public void onPause()
	{
		mListener.onInstrumentSelect(mSelected);
		super.onPause();
	}

}
