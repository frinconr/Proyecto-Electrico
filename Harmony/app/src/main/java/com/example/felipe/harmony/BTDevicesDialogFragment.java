package com.example.felipe.harmony;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ListAdapter;

public class BTDevicesDialogFragment extends DialogFragment {

    private ListAdapter mListAdapter;

    public interface NoticeDialogListener {
        void onClickBTDevices (DialogFragment dialog, int chosenDevice);
    }

    NoticeDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public @NonNull Dialog onCreateDialog(final Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.pick_device);

        builder.setAdapter(mListAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mListener.onClickBTDevices(BTDevicesDialogFragment.this,id);
            }
        });
        return builder.create();
    }

    public void setListAdapter(ListAdapter mListAdapter){
        this.mListAdapter = mListAdapter;
    }

}
