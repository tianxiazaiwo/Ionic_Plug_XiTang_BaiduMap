package com.sdr.sluicemapplugin;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import com.sdr.xitang.R;

import com.sdr.sluicemapplugin.activities.ZhaZhanFenBu;
import com.sdr.sluicemapplugin.activities.ZhaZhanZhuangTai;
import com.sdr.sluicemapplugin.bean.WaterGateBean;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button fenbu,zhuangtai;

    private Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
    }

    private void initView() {
        fenbu= (Button) findViewById(R.id.btn_ZhaZhanFenBu);
        zhuangtai = (Button) findViewById(R.id.btn_ZhaZhanZhuangTai);
    }

    private void initListener() {
        fenbu.setOnClickListener(this);
        zhuangtai.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_ZhaZhanFenBu:
                intent = new Intent(this,ZhaZhanFenBu.class);
                Intent intent = new Intent(this, ZhaZhanFenBu.class);
                intent.putExtra("userId","42");
                intent.putExtra("accessToken","d345824fb4c344098afa5f399ddf84cb");
                intent.putExtra("serverUri","http://192.168.0.72:8090/xitang-rest/");
                startActivity(intent);
                break;
            case R.id.btn_ZhaZhanZhuangTai:
                intent = new Intent(this,ZhaZhanZhuangTai.class);
                WaterGateBean waterGate = new WaterGateBean();

                intent.putExtra("userId","41");
                intent.putExtra("accessToken","40768c6db3934fc6907fd776ba7337c4");
                intent.putExtra("serverUri","http://192.168.0.72:8090/xitang-rest/");

                waterGate.setLGTD(120.881569);
                waterGate.setLTTD(30.97184);
                waterGate.setVideoCount(3);
                waterGate.setId(2);
                intent.putExtra("waterGate",waterGate);

                startActivity(intent);
                break;
        }
    }
}
