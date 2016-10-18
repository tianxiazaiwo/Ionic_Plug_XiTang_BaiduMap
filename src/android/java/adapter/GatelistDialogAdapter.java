package com.sdr.sluicemapplugin.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.sdr.xitang.R;
import com.sdr.sluicemapplugin.activities.ZhaZhanZhuangTai;
import com.sdr.sluicemapplugin.bean.WaterGateBean;

import java.util.List;

/**
 * Created by HeYongFeng on 2016/10/14.
 */
public class GatelistDialogAdapter extends BaseAdapter{

    private Context context;
    List<WaterGateBean.GateListBean> gateList;
    private String userId;
    private String accessToken;
    private double latitude;
    private double longitude;
    private String serverUri;
    private int videoCount;

    public GatelistDialogAdapter(Context context, List<WaterGateBean.GateListBean> gateList, String userId, String accessToken, double latitude, double longitude,String serverUri,int videoCount) {
        this.context = context;
        this.gateList = gateList;
        this.userId = userId;
        this.accessToken = accessToken;
        this.latitude = latitude;
        this.longitude = longitude;
        this.serverUri = serverUri;
        this.videoCount = videoCount;
    }

    @Override
    public int getCount() {
        return gateList.size();
    }

    @Override
    public Object getItem(int position) {
        return gateList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        convertView = View.inflate(context, R.layout.customer_dialog_listview_item,null);
        TextView tv_dialog_lv_item_name = (TextView) convertView.findViewById(R.id.tv_dialog_lv_item_name);
        TextView tv_dialog_lv_item_status = (TextView) convertView.findViewById(R.id.tv_dialog_lv_item_status);
        tv_dialog_lv_item_name.setText(gateList.get(position).getName());
        if (gateList.get(position).getStatus() == 0){
            tv_dialog_lv_item_status.setText("(关闭)");
        }else {
            tv_dialog_lv_item_status.setText("(开启)");
        }
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ZhaZhanZhuangTai.class);
                intent.putExtra("userId",userId);
                intent.putExtra("accessToken",accessToken);
                intent.putExtra("SGPCD",gateList.get(position).getSgpcd());
                intent.putExtra("latitude",latitude);
                intent.putExtra("longitude",longitude);
                intent.putExtra("name",gateList.get(position).getName());
                intent.putExtra("serverUri",serverUri);
                intent.putExtra("videoCount",videoCount);
                context.startActivity(intent);
            }
        });
        return convertView;
    }
}
