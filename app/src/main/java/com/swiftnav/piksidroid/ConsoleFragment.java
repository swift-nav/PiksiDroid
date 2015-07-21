package com.swiftnav.piksidroid;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;

import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.logging.MsgLog;
import com.swiftnav.sbp.SBPMessage;

public class ConsoleFragment extends Fragment implements PiksiListener {
	View view;
	SBPHandler piksiHandler;
	EditText mConsole;
	ScrollView mScrollView;

	public ConsoleFragment() {}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_console, container, false);
		mConsole = ((EditText) view.findViewById(R.id.console));
		if (piksiHandler == null)
			mConsole.setText("Piksi not connected!");
		mConsole.setTextIsSelectable(false);
		mConsole.setClickable(false);

		mScrollView = (ScrollView)view.findViewById(R.id.scrollView);
		mScrollView.setClickable(false);
		mScrollView.setFocusable(false);
		mScrollView.setOnTouchListener(null);
		mScrollView.setPressed(false);

		return view;
	}

	@Override
	public void piksiConnected(SBPHandler handler) {
		mConsole.setText("");
		piksiHandler = handler;
		piksiHandler.addCallback(MsgLog.TYPE, printCallback);
	}

	public SBPCallback printCallback = new SBPCallback() {
		@Override
		public void receiveCallback(SBPMessage msg) {
			final MsgLog message = (MsgLog)msg;
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (mConsole.getLineCount() > 100)
						mConsole.setText("");
					mConsole.append(message.text);
					mScrollView.post(new Runnable() {
						@Override
						public void run() {
							mScrollView.smoothScrollTo(0, mConsole.getBottom());
						}
					});
				}
			});
		}
	};
}
