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
import com.swiftnav.sbp.logging.MsgPrint;
import com.swiftnav.sbp.SBPMessage;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConsoleFragment extends Fragment {

	View view;
	SBPHandler piksiHandler;

	public ConsoleFragment() {
		// Required empty public constructor
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		view = inflater.inflate(R.layout.fragment_console, container, false);
		return view;
	}

	public void fixFragment(SBPHandler handler) {
		this.piksiHandler = handler;
		piksiHandler.addCallback(MsgPrint.TYPE, printCallback);
	}

	public SBPCallback printCallback = new SBPCallback() {
		@Override
		public void receiveCallback(SBPMessage msg) {
			final MsgPrint message = (MsgPrint)msg;
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					EditText console = (EditText) getActivity().findViewById(R.id.console);
					final ScrollView sv = (ScrollView) getActivity().findViewById(R.id.scrollView);
					if (console.getLineCount() > 100)
						console.setText("");
					console.append(message.text);
					final EditText c = console;
					sv.post(new Runnable() {
						@Override
						public void run() {
							sv.smoothScrollTo(0, c.getBottom());
						}
					});
				}
			});
		}
	};
}
