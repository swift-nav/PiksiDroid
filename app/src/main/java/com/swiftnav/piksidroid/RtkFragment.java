package com.swiftnav.piksidroid;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.swiftnav.sbp.SBPMessage;
import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.client.SBPHandler;
import com.swiftnav.sbp.navigation.MsgBaselineNED;

/**
 * A simple {@link Fragment} subclass.
 */
public class RtkFragment extends Fragment {
	View view;
	SBPHandler piksiHandler;
	TextView n,e,d,tow,n_sats, ff;

	public RtkFragment() {
		// Required empty public constructor
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_rtk, container, false);

		n = ((TextView)view.findViewById(R.id.n));
		e = ((TextView)view.findViewById(R.id.e));
		d = ((TextView)view.findViewById(R.id.d));
		tow = ((TextView)view.findViewById(R.id.tow));
		n_sats = ((TextView)view.findViewById(R.id.n_sats));
		ff = ((TextView)view.findViewById(R.id.ff));

		n.setText("North: ");
		e.setText("East: ");
		d.setText("Down: ");
		tow.setText("TOW: ");
		n_sats.setText("# of sats: ");
		ff.setText("Status: ");

		return view;
	}


	public void fixFragment(SBPHandler handler) {
		piksiHandler = handler;

		piksiHandler.add_callback(MsgBaselineNED.TYPE, baselineNEDCallback);
	}

	public SBPCallback baselineNEDCallback = new SBPCallback() {
		@Override
		public void receiveCallback(SBPMessage sbpMessage) {
			final SBPMessage msg = sbpMessage;
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					MsgBaselineNED baselineMsg = null;
					try {
						baselineMsg = new MsgBaselineNED(msg);
					} catch (Exception e) {
						e.printStackTrace();
					}

					int e_ = baselineMsg.e;
					int n_ = baselineMsg.n;
					int flag = baselineMsg.flags;
					long tow_ = baselineMsg.tow;
					int d_ = baselineMsg.d;
					int n_sats_ = baselineMsg.n_sats;

					if (flag == 1)
						ff.setText("Status: Fixed");
					else
						ff.setText("Status: Float");

					e.setText("East: " + e_);
					n.setText("North: " + n_);
					d.setText("Down: " + d_);
					tow.setText("TOW: " + tow_);
					n_sats.setText("# of sats: " + n_sats_);
				}
			});
		}
	};
}
