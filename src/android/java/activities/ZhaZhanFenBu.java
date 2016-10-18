package com.sdr.sluicemapplugin.activities;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.LocationClient;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Projection;
import com.baidu.mapapi.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sdr.xitang.R;
import com.sdr.sluicemapplugin.adapter.GatelistDialogAdapter;
import com.sdr.sluicemapplugin.bean.PersonPosition;
import com.sdr.sluicemapplugin.bean.WaterGateBean;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ZhaZhanFenBu extends AppCompatActivity{

    private DrawerLayout drawerLayout;
    private ListView leftListView;
    private ArrayList<WaterGateBean> waterGateList;
    private ArrayList<PersonPosition> PersonPositionList;

    private ArrayList<String> leftWaterGateList;
    private ArrayAdapter<String> adapter;

    //地图相关
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    //水闸覆盖物的布局
    private View markerView;
    private TextView infoWindowText;
    private List<Marker> waterGateMakerList = new ArrayList<Marker>();
    private List<Marker> onlyWaterGateMarkerList = new ArrayList<Marker>();
    private List<Marker> personMarkerList = new ArrayList<Marker>();
    private int currentPosition = -1;
    private Timer mTimer;


    //地图定位相关
    private LocationClient mLocationClient;
    private boolean isFirstIn = true;

    //访问网络相关
    private String waterGateUrl = "rest/station/getAllStationGatePumb";
    private String personPositionUrl = "rest/user/getUserLocation";
    private String userId;
    private String accessToken;
    private String serverUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //初始化地图
        SDKInitializer.initialize(getApplicationContext());
        setContentView(getApplication().getResources()
                .getIdentifier("activity_zha_zhan_fen_bu", "layout", getApplication().getPackageName()));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.baidumap_actionbar_custom);
        ((TextView)actionBar.getCustomView().findViewById(R.id.tv_actionbar_title)).setText("闸站分布");
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#72D3E4")));
        actionBar.getCustomView().findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            this.userId = intent.getStringExtra("userId");
            this.accessToken = intent.getStringExtra("accessToken");
            this.waterGateUrl = intent.getStringExtra("serverUri") + this.waterGateUrl;
            this.personPositionUrl = intent.getStringExtra("serverUri") + this.personPositionUrl;
            this.serverUri = intent.getStringExtra("serverUri");
        }
        initView();
        initData();
        initListener();
//        initLocationService();
    }


    private void initView() {
        drawerLayout = (DrawerLayout) findViewById(getApplication().getResources()
                .getIdentifier("drawer_layout", "id", getApplication().getPackageName()));
        leftListView = (ListView) findViewById(getApplication().getResources()
                .getIdentifier("left_drawer", "id", getApplication().getPackageName()));

        //加载覆盖物的布局
        markerView = View.inflate(this, getApplication().getResources()
                .getIdentifier("marker_infowindow", "layout", getApplication().getPackageName()), null);
        infoWindowText = (TextView) markerView.findViewById(getApplication().getResources()
                .getIdentifier("tv_infoWindow", "id", getApplication().getPackageName()));

        //地图相关
        mMapView = (MapView) findViewById(getApplication().getResources()
                .getIdentifier("mv_ZhaZhanFenBu", "id", getApplication().getPackageName()));
        mBaiduMap = mMapView.getMap();
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(14.0f);
        mBaiduMap.setMapStatus(msu);
    }

    //获取网络数据
    private void initData() {

        waterGateList = new ArrayList<WaterGateBean>();
        PersonPositionList = new ArrayList<PersonPosition>();
        mBaiduMap.setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                getInternetData();
                //创建定时器
                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.e("定时器", "定时器响应了.....");
                        MyTask2 myTask = new MyTask2();
                        myTask.execute();
                    }
                }, 150, 30000);
            }
        });
    }

    //初始化监听事件
    private void initListener() {
        //左侧菜单栏的选项点击事件
        leftListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LatLng latLng = new LatLng(waterGateList.get(position).getLTTD(), waterGateList.get(position).getLGTD());
                if (mBaiduMap.getMapStatus().zoom >= 14) {
                    //设置之前的Marker为白色
//                    infoWindowText.setText(waterGateList.get(currentPosition).getName());
//                    infoWindowText.setTextColor(Color.parseColor("#000000"));
//                    infoWindowText.setBackgroundResource(getApplication().getResources()
//                            .getIdentifier("infowindow_kuang", "drawable", getApplication().getPackageName()));
//                    BitmapDescriptor descriptorBefor = BitmapDescriptorFactory.fromView(markerView);
//                    waterGateMakerList.get(currentPosition).setIcon(descriptorBefor);
//                    waterGateMakerList.get(currentPosition).setZIndex(0);
//                    //设置之后的背景颜色为蓝色
//                    infoWindowText.setText(waterGateList.get(position).getName());
//                    infoWindowText.setTextColor(Color.parseColor("#ffffff"));
//                    infoWindowText.setBackgroundResource(getApplication().getResources()
//                            .getIdentifier("infowindow_select_kuang", "drawable", getApplication().getPackageName()));
//                    BitmapDescriptor descriptorLater = BitmapDescriptorFactory.fromView(markerView);
//                    waterGateMakerList.get(position).setIcon(descriptorLater);
//                    waterGateMakerList.get(position).setZIndex(1);
//                    currentPosition = position;
                    markClickEnd(waterGateList.get(position),position,waterGateMakerList.get(position));
                }
                moveToPlace(latLng);
                drawerLayout.closeDrawers();
            }
        });

        //地图缩放的监听事件
        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus) {

            }

            @Override
            public void onMapStatusChange(MapStatus mapStatus) {

            }

            @Override
            public void onMapStatusChangeFinish(MapStatus mapStatus) {
                if (mapStatus.zoom >= 14.0) {
                    if (waterGateMakerList.isEmpty()) {
                        if (!onlyWaterGateMarkerList.isEmpty()) {
                            for (int i = 0; i < onlyWaterGateMarkerList.size(); i++) {
                                onlyWaterGateMarkerList.get(i).remove();
                                if (i == onlyWaterGateMarkerList.size() - 1) {
                                    onlyWaterGateMarkerList.clear();
                                }
                            }
                        }
                        addWaterGateAndInfo();
                    }
                } else if (!waterGateMakerList.isEmpty()) {
                    for (int i = 0; i < waterGateMakerList.size(); i++) {
                        waterGateMakerList.get(i).remove();
                        if (i == waterGateMakerList.size() - 1) {
                            waterGateMakerList.clear();
                            //只添加水闸覆盖物
                            addWaterGateMarker();
                        }
                    }
                }
            }
        });

        //点击地图上覆盖物的监听事件
        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Bundle bundle = marker.getExtraInfo();
                if (bundle != null) {
                    //说明是水闸位置的覆盖物
                    WaterGateBean waterGate = (WaterGateBean) bundle.getSerializable("waterGateBean");
                    if (!waterGateMakerList.isEmpty()) {
                        //说明显示有信息栏
                        markClickEnd(waterGate,bundle.getInt("position"),marker);

                        List<WaterGateBean.GateListBean> gateList = waterGate.getGateList();
                        if (!gateList.isEmpty()) {
                            Dialog dialog = new Dialog(ZhaZhanFenBu.this, R.style.GatelistDialog);
                            View gateListView = View.inflate(ZhaZhanFenBu.this, getApplication().getResources()
                                    .getIdentifier("customer_dialog_showgatelist", "layout", getApplication().getPackageName()), null);
                            ListView lv_gateListView = (ListView) gateListView.findViewById(R.id.lv_gateListView);
                            //获取到它的闸门/水泵的信息
                            Log.e("获取的闸门/水泵的信息", gateList.toString());

                            GatelistDialogAdapter dialogAdapter = new GatelistDialogAdapter(ZhaZhanFenBu.this, gateList,userId,accessToken,waterGate.getLTTD(),waterGate.getLGTD(),serverUri,waterGate.getVideoCount());
                            lv_gateListView.setAdapter(dialogAdapter);
                            dialog.setContentView(gateListView);
                            dialog.setCancelable(true);
                            dialog.show();

                            //设置dialog的宽高
                            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
                            params.width = getScreenWidth() / 3 * 2;
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                            dialog.getWindow().setAttributes(params);
                        }else {
                            Toast.makeText(ZhaZhanFenBu.this, "该闸站没有闸门/泵站", Toast.LENGTH_SHORT).show();
                        }
                        //跳转
//                        Intent intent = new Intent(ZhaZhanFenBu.this, ZhaZhanZhuangTai.class);
//                        intent.putExtra("userId", userId);
//                        intent.putExtra("accessToken", accessToken);
//                        intent.putExtra("serverUri", serverUri);
//                        intent.putExtra("waterGate", waterGate);
//                        startActivity(intent);
                    }
                    moveToPlace(new LatLng(waterGate.getLTTD(), waterGate.getLGTD()));
                }
                return true;
            }
        });
    }

    private void markClickEnd(WaterGateBean waterGate, int position, Marker marker){
        if (waterGate.getIsMain() == 1) {

            if (currentPosition >= 0) {
                //设置之前的Marker为白色
                infoWindowText.setText(waterGateList.get(currentPosition).getName());
                infoWindowText.setTextColor(Color.parseColor("#000000"));
                infoWindowText.setBackgroundResource(getApplication().getResources()
                        .getIdentifier("infowindow_kuang", "drawable", getApplication().getPackageName()));
                BitmapDescriptor descriptorBefor = BitmapDescriptorFactory.fromView(markerView);
                waterGateMakerList.get(currentPosition).setIcon(descriptorBefor);
                waterGateMakerList.get(currentPosition).setZIndex(0);
            }
            //设置之后的背景颜色为蓝色
            infoWindowText.setText(waterGateList.get(position).getName());
            infoWindowText.setTextColor(Color.parseColor("#ffffff"));
            infoWindowText.setBackgroundResource(getApplication().getResources()
                    .getIdentifier("infowindow_select_kuang", "drawable", getApplication().getPackageName()));
            BitmapDescriptor descriptorLater = BitmapDescriptorFactory.fromView(markerView);
            marker.setIcon(descriptorLater);
            marker.setZIndex(1);
            currentPosition = position;
        }
    }

    private int getScreenWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

//    //初始化定位服务
//    private void initLocationService() {
//        mLocationClient = new LocationClient(this);
//        MyLocationListener myLocationListener = new MyLocationListener();
//        //注册监听
//        mLocationClient.registerLocationListener(myLocationListener);
//        //对定位进行相应的设置
//        LocationClientOption locationClientOption = new LocationClientOption();
//        locationClientOption.setCoorType("bd09ll");
//        locationClientOption.setIsNeedAddress(true);
//        locationClientOption.setOpenGps(true);
//        locationClientOption.setScanSpan(30000);
//        mLocationClient.setLocOption(locationClientOption);
//
//        //开启定位
//        mBaiduMap.setMyLocationEnabled(true);
//        if (!mLocationClient.isStarted()){
//            mLocationClient.start();
//        }
//    }

    //获取网络数据
    private void getInternetData() {
        MyTask myTask = new MyTask();
        myTask.execute();
    }


    //解析获取的json数据
    private void parseData(String result) {
        if (!result.startsWith("服务器异常")) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                int status = jsonObject.getInt("code");
                if (status == 200) {
                    result = jsonObject.getString("data");
                    Gson gson = new Gson();
                    Type type = new TypeToken<ArrayList<WaterGateBean>>() {
                    }.getType();
                    ArrayList<WaterGateBean> waterGateBeanList = gson.fromJson(result, type);
                    for (int i = 0; i < waterGateBeanList.size(); i++) {
                        waterGateList.add(waterGateBeanList.get(i));
                    }
                    //初始化左侧侧滑菜单的列表
                    initLeftMenu();
                    //此时说明数据已经获取到了，那么就可以进行添加覆盖物了
                    addOverlays();
                } else if (status == 202) {
                    Toast.makeText(ZhaZhanFenBu.this, "用户认证失败", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(ZhaZhanFenBu.this, result, Toast.LENGTH_SHORT).show();
        }
    }

    //解析巡检人员位置的信息
    private void parsePersonData(String s) {
        if (!s.startsWith("服务器异常")) {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(s);
                int status = jsonObject.getInt("code");
                if (status == 200) {
                    String result = jsonObject.getString("data");
                    Gson gson = new Gson();
                    Type type = new TypeToken<ArrayList<PersonPosition>>() {
                    }.getType();
                    ArrayList<PersonPosition> personList = gson.fromJson(result, type);
                    for (int i = 0; i < personList.size(); i++) {
                        PersonPositionList.add(personList.get(i));
                    }
                    if (isFirstIn) {
                        for (int i = 0; i < PersonPositionList.size(); i++) {
                            addPersonMarker(new LatLng(PersonPositionList.get(i).getLatitude(), PersonPositionList.get(i).getLongitude()));
                        }
                        isFirstIn = false;
                    }
                    //重新设置位置
                    for (int i = 0; i < personMarkerList.size(); i++) {
                        personMarkerList.get(i).setPosition(new LatLng(PersonPositionList.get(i).getLatitude(), PersonPositionList.get(i).getLongitude()));
                    }
                } else {
                    Toast.makeText(ZhaZhanFenBu.this, "认证失败", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
//            Toast.makeText(ZhaZhanFenBu.this, s, Toast.LENGTH_SHORT).show();
            Log.e("获取巡检人员信息",s);
        }
    }


    //初始化左侧菜单列表
    private void initLeftMenu() {
        leftWaterGateList = new ArrayList<String>();
        for (int i = 0; i < waterGateList.size(); i++) {
            leftWaterGateList.add(waterGateList.get(i).getName());
        }
        adapter = new ArrayAdapter<String>(this,
                getApplication().getResources().getIdentifier("simple_list_item", "layout",
                        getApplication().getPackageName()), leftWaterGateList);
        leftListView.setAdapter(adapter);
    }

    //添加覆盖物
    private void addOverlays() {
                //addCamera();
                //添加水闸位置、信息框覆盖物
                addWaterGateAndInfo();
                moveToPlace(new LatLng(30.946249,120.895486));
    }


    private void addWaterGateAndInfo() {
        for (int i = 0; i < waterGateList.size(); i++) {
            LatLng markerLatlng = new LatLng(waterGateList.get(i).getLTTD(), waterGateList.get(i).getLGTD());
            BitmapDescriptor descriptor = null;
            if (waterGateList.get(i).getIsMain() == 1) {
                infoWindowText.setText(waterGateList.get(i).getName());
                infoWindowText.setTextColor(Color.parseColor("#000000"));
                infoWindowText.setBackgroundResource(getApplication().getResources()
                        .getIdentifier("infowindow_kuang", "drawable", getApplication().getPackageName()));
                descriptor = BitmapDescriptorFactory.fromView(markerView);
            }else {
                descriptor = BitmapDescriptorFactory.fromResource(R.drawable.icon_map_mark);
            }

            OverlayOptions options = new MarkerOptions().icon(descriptor).position(markerLatlng);

            Marker waterGateMaker = (Marker) mBaiduMap.addOverlay(options);
            waterGateMaker.setZIndex(0);
            //存储位置信息
            Bundle bundle = new Bundle();
            bundle.putString("markerType", "waterGate");
            bundle.putInt("position", i);
            bundle.putSerializable("waterGateBean", waterGateList.get(i));
            waterGateMaker.setExtraInfo(bundle);
            waterGateMakerList.add(waterGateMaker);
        }
    }

    //添加摄像头覆盖物
    private void addCamera() {
        for (int i = 0; i < waterGateList.size(); i++) {
            //添加摄像头的覆盖物
            for (int j = 0; j < waterGateList.get(i).getVideoCount(); j++) {
                Projection projection = mBaiduMap.getProjection();
                Point p = projection.toScreenLocation(new LatLng(waterGateList.get(i).getLTTD(), waterGateList.get(i).getLGTD()));
                p.x -= Math.random() * (Math.random() > 0.5 ? 1 : -1) * 20;
                p.y -= Math.random() * (Math.random() > 0.5 ? 1 : -1) * 20;
                LatLng later = mBaiduMap.getProjection().fromScreenLocation(p);
                OverlayOptions camOptions = new MarkerOptions().position(later)
                        .icon(BitmapDescriptorFactory.fromResource(getApplication().getResources().getIdentifier("webcam", "drawable", getApplication().getPackageName())));
                Marker camMarker = (Marker) mBaiduMap.addOverlay(camOptions);
                camMarker.setZIndex(0);
            }
        }
    }

    //只添加水闸覆盖物
    private void addWaterGateMarker() {
        for (int i = 0; i < waterGateList.size(); i++) {
            LatLng waterGateLatlng = new LatLng(waterGateList.get(i).getLTTD(), waterGateList.get(i).getLGTD());
            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromResource(getApplication().getResources()
                    .getIdentifier("icon_map_mark", "drawable", getApplication().getPackageName()));
            OverlayOptions options = new MarkerOptions().icon(descriptor).position(waterGateLatlng);
            Marker waterGateMaker = (Marker) mBaiduMap.addOverlay(options);
            waterGateMaker.setZIndex(0);
            //存储位置信息
            Bundle bundle = new Bundle();
            bundle.putString("markerType", "waterGate");
            bundle.putSerializable("waterGateBean", waterGateList.get(i));
            waterGateMaker.setExtraInfo(bundle);
            onlyWaterGateMarkerList.add(waterGateMaker);
        }
    }

    //添加巡检人员位置覆盖物
    private void addPersonMarker(LatLng latLng) {
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromResource(getApplication().getResources()
                .getIdentifier("current_position", "drawable", getApplication().getPackageName()));
        OverlayOptions options = new MarkerOptions().icon(descriptor).position(latLng);
        Marker personMarker = (Marker) mBaiduMap.addOverlay(options);
        personMarker.setZIndex(0);
        personMarkerList.add(personMarker);
    }


    //将地图移动到指定的位置
    private void moveToPlace(LatLng latLng) {
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.animateMapStatus(msu, 500);
    }

    //管理地图的生命周期
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (mLocationClient.isStarted()){
//            mLocationClient.stop();
//        }
//        mBaiduMap.setMyLocationEnabled(false);
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }


    class MyTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            HttpPost post = new HttpPost(waterGateUrl);
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);
            List<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
            pairs.add(new BasicNameValuePair("userId", userId));
            pairs.add(new BasicNameValuePair("accessToken", accessToken));
            try {
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs);
                post.setEntity(entity);
                HttpResponse httpResponse = httpClient.execute(post);
                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    String result = getTextFromStream(httpResponse.getEntity().getContent());
                    return result;
                } else {
                    return "服务器异常";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "服务器异常";
        }

        @Override
        protected void onPostExecute(String s) {
            Log.e("获取闸站位置结果", s);
            parseData(s);
        }
    }

    class MyTask2 extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            HttpPost httpPost = new HttpPost(personPositionUrl);
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);
            List<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
            pairs.add(new BasicNameValuePair("userId", userId));
            pairs.add(new BasicNameValuePair("accessToken", accessToken));
            try {
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs);
                httpPost.setEntity(entity);
                HttpResponse response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    String result = getTextFromStream(response.getEntity().getContent());
                    return result;
                } else {
                    return "服务器异常";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "服务器异常";
        }

        @Override
        protected void onPostExecute(String s) {
            parsePersonData(s);
            Log.e("获取巡检人员位置结果", s);
        }
    }


    public String getTextFromStream(InputStream is) {
        byte[] b = new byte[1024];
        int len = 0;
        //创建字节数组输出流，读取输入流的文本数据时，同步把数据写入数组输出流
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            while ((len = is.read(b)) != -1) {
                bos.write(b, 0, len);
            }
            //把字节数组输出流里的数据转换成字节数组
            String text = new String(bos.toByteArray());
            return text;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
