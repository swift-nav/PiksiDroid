package com.swiftnav.sbp.loggers;

import android.util.Base64;
import android.util.Log;

import com.swiftnav.sbp.client.SBPCallback;
import com.swiftnav.sbp.msg.SBPMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.TimeZone;

public class JSONLogger implements SBPCallback {
    static final String TAG = "JSONLogger";
    private OutputStream stream;
    private long starttime;

    public JSONLogger(OutputStream stream_) {
        stream = stream_;
        starttime = utc();
    }

    private long utc() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        return cal.getTimeInMillis() / 1000;
    }

    private long delta() {
        return utc() - starttime;
    }

    @Override
    public void receiveCallback(SBPMessage msg) {
        JSONObject logobj = new JSONObject();
        try {
            logobj.put("delta", delta());
            logobj.put("timestamp", utc());
            JSONObject msgobj = new JSONObject();
            msgobj.put("msg_type", msg.type);
            msgobj.put("sender", msg.sender);
            byte[] payload = msg.getPayload();
            msgobj.put("length", payload.length);
            msgobj.put("payload", Base64.encodeToString(payload, Base64.DEFAULT));
            logobj.put("data", msgobj);
        } catch (JSONException e) {
            Log.e(TAG, "Error encoding JSON object: " + e.toString());
        }

        try {
            stream.write((logobj.toString() + "\n").getBytes());
        } catch (IOException e) {
            Log.e(TAG, "IOException writing JSON log: " + e.toString());
        }
    }
}
