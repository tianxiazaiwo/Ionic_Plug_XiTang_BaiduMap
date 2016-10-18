package com.sdr.sluicemapplugin.plugin;

import android.content.Intent;

import com.sdr.sluicemapplugin.activities.ZhaZhanFenBu;
import com.sdr.sluicemapplugin.activities.ZhaZhanZhuangTai;
import com.sdr.sluicemapplugin.bean.WaterGateBean;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by homelajiang on 2016/9/2 0002.
 */
public class SluiceMapManager extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("sluices")) {

            Intent intent = new Intent(cordova.getActivity(), ZhaZhanFenBu.class);
            intent.putExtra("userId",args.getString(0));
            intent.putExtra("accessToken",args.getString(1));
            intent.putExtra("serverUri",args.getString(2));


            cordova.getActivity().startActivity(intent);
            return true;
        }

        if (action.equals("sluice")) {
            Intent intent = new Intent(cordova.getActivity(), ZhaZhanZhuangTai.class);
            WaterGateBean waterGate = new WaterGateBean();

            intent.putExtra("userId",args.getString(0));
            intent.putExtra("accessToken",args.getString(1));
            intent.putExtra("serverUri",args.getString(2));

            waterGate.setId(args.getInt(3));
            waterGate.setLGTD(args.getDouble(4));
            waterGate.setLTTD(args.getDouble(5));
            waterGate.setVideoCount(args.getInt(6));

            intent.putExtra("waterGate",waterGate);

            cordova.getActivity().startActivity(intent);
            return true;
        }

        return false;
    }
}
