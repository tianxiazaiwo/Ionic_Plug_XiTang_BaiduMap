package com.sdr.sluicemapplugin.activities;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.bumptech.glide.Glide;
import com.sdr.xitang.R;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ZhaZhanZhuangTai extends AppCompatActivity {

    private RadioGroup rg_waterGateStatus;
    private EditText edt_WaterGateDes;
    private GridView gv_photos;
    private Button btn_upload;
    private ArrayList<String> imagePathList;
    private ArrayList<String> cacheImageList;

    private String status="";
    private ShowPhotosAdapter mAdapter;

    //Android6.0申请权限之后的返回码
    public static final int TAKE_PHOTO_REQUEST_CODE=200;
    public static final int OPEN_ALBUM_REQUEST_CODE=300;

    //照片的地址
    private File outputImage;

    //百度地图相关
    private MapView mv_ZhaZhanMapView;
    private BaiduMap mBaiduMap;
    private int screenWidth;//屏幕的宽度
    private int screenHeight;//屏幕的高度

    //自定义的Dialog
    private Dialog mDialog;

    //访问网络相关
    String url = "upload/uploadStationData";
    //闸门的id
    String userId;
    String accessToken;
    private double latitude;
    private double longitude;
    private String gateName;
    private int videoCount;
    private String SGPCD;
//    private ProgressDialog pd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(getApplication().getResources()
                        .getIdentifier("activity_zha_zhan_zhuang_tai","layout",getApplication().getPackageName()));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.baidumap_actionbar_custom);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#72D3E4")));
        ((TextView)actionBar.getCustomView().findViewById(R.id.tv_actionbar_title)).setText("闸站状态");
        actionBar.getCustomView().findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        initView();
        initData();
        initListener();
    }
    private void initView() {
        Intent intent = getIntent();
        if(intent!=null){
            this.userId = intent.getStringExtra("userId");
            this.accessToken = intent.getStringExtra("accessToken");
            this.latitude = intent.getDoubleExtra("latitude",0);
            this.longitude = intent.getDoubleExtra("longitude",0);
            this.gateName = intent.getStringExtra("name");
            this.videoCount = intent.getIntExtra("videoCount",0);
            this.SGPCD = intent.getStringExtra("SGPCD");
            this.url = intent.getStringExtra("serverUri")+this.url;
        }
        rg_waterGateStatus= (RadioGroup) findViewById(getApplication().getResources()
                        .getIdentifier("rg_waterGateStatus","id",getApplication().getPackageName()));
        edt_WaterGateDes= (EditText) findViewById(getApplication().getResources()
                        .getIdentifier("edt_WaterGateDes","id",getApplication().getPackageName()));
        gv_photos= (GridView) findViewById(getApplication().getResources()
                        .getIdentifier("gv_photos","id",getApplication().getPackageName()));
        btn_upload= (Button) findViewById( getApplication().getResources()
                        .getIdentifier("btn_upload","id",getApplication().getPackageName()));

        //初始化地图相关
        mv_ZhaZhanMapView= (MapView) findViewById(getApplication().getResources()
                        .getIdentifier("mv_ZhaZhanMapView","id",getApplication().getPackageName()));
        mv_ZhaZhanMapView.showZoomControls(false);
        mv_ZhaZhanMapView.setScrollContainer(false);
        mv_ZhaZhanMapView.setClickable(false);
        mv_ZhaZhanMapView.setLongClickable(false);
        //获取手机屏幕的宽高
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        // 屏幕宽度（像素）
        screenWidth = metric.widthPixels;
        // 屏幕高度（像素）
        screenHeight = metric.heightPixels;
//        float density = metric.density;      // 屏幕密度（0.75 / 1.0 / 1.5）
//        int densityDpi = metric.densityDpi;  // 屏幕密度DPI（120 / 160 / 240）
        //设置百度地图的宽高
        mv_ZhaZhanMapView.setLayoutParams(new LinearLayout.LayoutParams(screenWidth,screenHeight/5*2));

        mBaiduMap=mv_ZhaZhanMapView.getMap();
        mBaiduMap.setMaxAndMinZoomLevel(17.0f,17.0f);
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(17.0f);
        mBaiduMap.setMapStatus(msu);
//        //隐藏键盘
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void initData() {
        imagePathList=new ArrayList<String>();
        cacheImageList=new ArrayList<String>();
        mAdapter = new ShowPhotosAdapter(this,imagePathList);
        gv_photos.setAdapter(mAdapter);
    }

    private void initListener() {
        rg_waterGateStatus.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId==getApplication().getResources()
                        .getIdentifier("rb_open","id",getApplication().getPackageName())){
                    status="1";
                }
                if (checkedId==getApplication().getResources()
                        .getIdentifier("rb_close","id",getApplication().getPackageName())){
                    status="-1";
                }
            }
        });


        mBaiduMap.setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mBaiduMap.setOnMapTouchListener(null);
                mBaiduMap.setOnMapDoubleClickListener(null);
                mBaiduMap.setOnMapClickListener(null);
                //将地图中心点移动至闸站位置
                    LatLng latLng = new LatLng(latitude,longitude);
                    //添加覆盖物和摄像头的位置
                    View view = View.inflate(ZhaZhanZhuangTai.this, R.layout.marker_infowindow,null);
                    TextView text = (TextView)view.findViewById(R.id.tv_infoWindow);
                    text.setText(gateName);
                    text.setTextColor(Color.parseColor("#737373"));
                    BitmapDescriptor waterGateDescriptor = BitmapDescriptorFactory.fromView(view);
//                    BitmapDescriptor camDescriptor = BitmapDescriptorFactory.fromResource(getApplication().getResources()
//                                    .getIdentifier("webcam","drawable",getApplication().getPackageName()));
                    OverlayOptions waterGateOptions = new MarkerOptions().icon(waterGateDescriptor).position(latLng);
//                    Point point = mBaiduMap.getProjection().toScreenLocation(latLng);
//                    for (int i = 0; i < videoCount; i++) {
//                        point.x-=Math.random()*(Math.random()>0.5?1:-1)*50;
//                        point.y-=Math.random()*(Math.random()>0.5?1:-1)*50;
//                        LatLng camLatLng = mBaiduMap.getProjection().fromScreenLocation(point);
//                        OverlayOptions camOptions = new MarkerOptions().icon(camDescriptor).position(camLatLng);
//                        mBaiduMap.addOverlay(camOptions);
//                    }
                    mBaiduMap.addOverlay(waterGateOptions);
                    MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
                    mBaiduMap.animateMapStatus(msu);
            }
        });

        //点击上传按钮的监听
        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //判断是否符合上传的条件
                if (!"".equals(status)){
                    if (!"".equals(edt_WaterGateDes.getText().toString())){
                        if (!imagePathList.isEmpty()){
                            //判断通过，可以进行上传
                            MyTask myTask = new MyTask();
                            myTask.execute();
                        }else {
                            Toast.makeText(ZhaZhanZhuangTai.this, "请选择照片后再上传", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        Toast.makeText(ZhaZhanZhuangTai.this, "请填写闸门描述", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(ZhaZhanZhuangTai.this, "请选择闸门状态", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //上传数据
    private String startUpLoadData() {
                HttpPost httpPost = new HttpPost(url);
                HttpClient httpClient = new DefaultHttpClient();
                //设置请求超时时间
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);
        MultipartEntity entity = new MultipartEntity();
                try {
                    entity.addPart("userId",new StringBody(userId));
                    entity.addPart("accessToken",new StringBody(accessToken));
                    entity.addPart("status",new StringBody(status));
                    entity.addPart("SGPCD",new StringBody(SGPCD));
                    entity.addPart("sm",new StringBody(edt_WaterGateDes.getText().toString()));
                    for (int i = 0; i < cacheImageList.size(); i++) {
                        entity.addPart("file",new FileBody(new File(cacheImageList.get(i))));
                    }
                    httpPost.setEntity(entity);
                    HttpResponse response = httpClient.execute(httpPost);
                    if (response.getStatusLine().getStatusCode()== HttpStatus.SC_OK){
                        InputStream is= response.getEntity().getContent();
                        byte [] b =new byte[1024];
                        int len = 0;
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        while((len=is.read(b))!=-1){
                            bos.write(b,0,len);
                        }
                        is.close();
                        bos.close();
                        String result = new String(bos.toByteArray());
                        JSONObject jsonObject = new JSONObject(result);
                        int statusCode = jsonObject.getInt("code");
                        if (statusCode==200){
                            return "上传成功";
                        }else {
                            return "上传失败";
                        }
                    }else {
                        return "网络异常";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
        return "上传失败";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==TAKE_PHOTO_REQUEST_CODE) {
            if (outputImage.length()>0){
                String imagePath = outputImage.getAbsolutePath();
                imagePathList.add(imagePath);
                mAdapter.notifyDataSetChanged();
                //创建缓存
                createImageCache(outputImage);
            }else {
                outputImage.delete();
            }
        }
        if (requestCode == OPEN_ALBUM_REQUEST_CODE){
            if (data != null){
                try {
                Uri uri = data.getData();
                String[] proj = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
                if(cursor.moveToFirst()) {
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    String path = cursor.getString(column_index);
                    imagePathList.add(path);
                    mAdapter.notifyDataSetChanged();
                    createImageCache(new File(path));
                }
                cursor.close();
            } catch (Exception e) {
                Log.e("TAG-->Error", e.toString());
            }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //图片缓存
    private void createImageCache(File outputImage) {
        //拍照图片的名字
        SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date date = new Date(System.currentTimeMillis());
        String fileName = format.format(date);

        File cacheFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"cache"+File.separator+"Cache_"+fileName+".jpeg");
        try {
            if(cacheFile.exists()) {
                cacheFile.delete();
            }
            cacheFile.createNewFile();
        } catch(IOException e) {
            e.printStackTrace();
        }
        //开始进行压缩图片
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        Bitmap bitmapBefor = BitmapFactory.decodeFile(outputImage.getAbsolutePath(),options);
        //获取原图片的宽高
        int widthBig = options.outWidth;
        int heightBig = options.outHeight;
        //获取屏幕的宽高
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthSmall = metrics.widthPixels;
        int heightSmall = metrics.heightPixels;

        int widthScale = widthBig/widthSmall;
        int heightScale = heightBig/heightSmall;

        int scale = 1;
        if (widthScale>=heightScale && widthScale>=1){
            scale = widthScale;
        }else if (widthScale<heightScale && heightScale>=1){
            scale = heightScale;
        }
        options.inSampleSize=scale;
        options.inJustDecodeBounds=false;
        Bitmap bitmapLater = BitmapFactory.decodeFile(outputImage.getAbsolutePath(),options);

        //将bitmap转化为File文件
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(cacheFile));
            bitmapLater.compress(Bitmap.CompressFormat.JPEG, 60, bos);

            //将缓存路径添加到集合中
            cacheImageList.add(cacheFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    //6.0申请权限成功之后的回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==TAKE_PHOTO_REQUEST_CODE){
            startSystemCamera();
        }
        if (requestCode == OPEN_ALBUM_REQUEST_CODE){
            startSystemAlbum();
        }
    }

    private void startSystemCamera(){
        //拍照图片的名字
        SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date date = new Date(System.currentTimeMillis());
        String fileName = format.format(date);
        //创建File对象用于存储拍照的图片 SD卡根目录
        //File outputImage = new File(Environment.getExternalStorageDirectory(),test.jpg);
        //存储至DCIM文件夹
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        outputImage = new File(path,fileName+".jpg");
        try {
            if(outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch(IOException e) {
            e.printStackTrace();
        }
        //将File对象转换为Uri并启动照相程序
        Uri imageUri = Uri.fromFile(outputImage);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //照相
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri); //指定图片输出地址
        startActivityForResult(intent,TAKE_PHOTO_REQUEST_CODE); //启动照相
        //拍完照startActivityForResult() 结果返回onActivityResult()函数
    }

    private void startSystemAlbum(){

        boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        Intent intent = null;

        if (isKitKat){
            intent = new Intent(Intent.ACTION_PICK);
        }else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.setType("image/*");
        /* 取得相片后返回本画面 */
        startActivityForResult(intent, OPEN_ALBUM_REQUEST_CODE);
    }

    //百度地图的生命周期
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mv_ZhaZhanMapView.onDestroy();
        //删除缓存图片
        if (!cacheImageList.isEmpty()){
            for (int i = 0; i < cacheImageList.size(); i++) {
                new File(cacheImageList.get(i)).delete();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mv_ZhaZhanMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mv_ZhaZhanMapView.onPause();
    }

    class ShowPhotosAdapter extends BaseAdapter {
        private Activity mContext;
        private ArrayList<String> imagePathList;

        public ShowPhotosAdapter(Activity context, ArrayList<String> imagePathList) {
            mContext = context;
            this.imagePathList = imagePathList;
        }

        @Override
        public int getCount() {
            return imagePathList.size()+1;
        }

        @Override
        public Object getItem(int position) {
            return imagePathList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
                convertView = View.inflate(mContext, getApplication().getResources()
                                .getIdentifier("item_gridview_takephotos","layout",getApplication().getPackageName()), null);
                ImageView iv_photo = (ImageView) convertView.findViewById(getApplication().getResources()
                                .getIdentifier("iv_photo","id",getApplication().getPackageName()));
                ImageView iv_delete = (ImageView) convertView.findViewById(getApplication().getResources()
                                .getIdentifier("iv_delete","id",getApplication().getPackageName()));
                iv_photo.setLayoutParams(new RelativeLayout.LayoutParams(screenWidth/3,screenWidth/3));
                if (position == imagePathList.size()) {
                    iv_photo.setBackgroundResource(getApplication().getResources()
                                    .getIdentifier("btn_add_images","drawable",getApplication().getPackageName()));
                    iv_delete.setVisibility(View.GONE);
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showPopwindow();
                        }
                    });
                }else if (!imagePathList.isEmpty()){
                    Glide.with(mContext).load(imagePathList.get(position)).into(iv_photo);
                    //删除图标的点击事件
                    iv_delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
//                            //将图片删除
//                            new File(imagePathList.get(position)).delete();
                            //删除缓存图片
                            new File(cacheImageList.get(position)).delete();
                            imagePathList.remove(position);
                            cacheImageList.remove(position);
                            notifyDataSetChanged();
                        }
                    });
                }
            return convertView;
        }
    }


    class MyTask extends AsyncTask<String,Integer,String>{

        @Override
        protected String doInBackground(String... params) {
            return startUpLoadData();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            Log.e("上传进度","【"+values+"】");
        }

        //当任务执行之前开始调用此方法，可以在这里显示进度对话框
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showCustomerDialog();
        }

        //执行完毕
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //让dialog消失
//            if (mDialog.isShowing()){
//                mDialog.dismiss();
//            }
            if(mDialog.isShowing()){
                mDialog.dismiss();
            }
            Toast.makeText(ZhaZhanZhuangTai.this, s, Toast.LENGTH_LONG).show();
            Log.e("返回结果", s);
            if (s.startsWith("上传成功")){
                //消除所有状态
                rg_waterGateStatus.clearCheck();
                edt_WaterGateDes.setText("");
                imagePathList.clear();
                for (int i = 0; i < cacheImageList.size(); i++) {
                    new File(cacheImageList.get(i)).delete();
                    if (i == cacheImageList.size()-1){
                        cacheImageList.clear();
                    }
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    private void showPopwindow(){
        View view = View.inflate(this,R.layout.popwindow_takephoto_album,null);

        final PopupWindow window = new PopupWindow(view, WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);
        window.setFocusable(true);
        setTransformBg(0.5f);
        // 实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(Color.parseColor("#00000000"));
        window.setBackgroundDrawable(dw);
        // 设置popWindow的显示和消失动画
        window.setAnimationStyle(R.style.ShowPopwindow);
        // 在底部显示
        window.showAtLocation(this.findViewById(R.id.ll_zhazhanzhuantai),
                Gravity.BOTTOM, 0, 0);
        //点击事件
        view.findViewById(R.id.tv_popwindow_album).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //6.0申请权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, ZhaZhanZhuangTai.OPEN_ALBUM_REQUEST_CODE);
                }else {
                    startSystemAlbum();
                }
                window.dismiss();
            }
        });

        view.findViewById(R.id.tv_popwindow_takephoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //6.0申请权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, ZhaZhanZhuangTai.TAKE_PHOTO_REQUEST_CODE);
                }else {
                    startSystemCamera();
                }
                window.dismiss();
            }
        });

        view.findViewById(R.id.tv_popwindow_cancle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                window.dismiss();
            }
        });

        window.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                setTransformBg(1.0f);
            }
        });
    }

    private void setTransformBg(float alpha){
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = alpha;
        getWindow().setAttributes(lp);
    }

    private void showCustomerDialog() {
//         pd = new ProgressDialog(this);
//        pd.setMessage("上传中，请稍等...");
//        pd.setCancelable(false);
//
//        if(!pd.isShowing()){
//            pd.show();
//        }
        //显示自定义的dialog
        mDialog = new Dialog(ZhaZhanZhuangTai.this,R.style.GatelistDialog);
        View v = View.inflate(this,getApplication().getResources()
                        .getIdentifier("customer_dialog","layout",getApplication().getPackageName()),null);
        //设置动画
        ImageView iv_dialogLoading = (ImageView)v.findViewById(getApplication().getResources()
                        .getIdentifier("iv_dialogLoading","id",getApplication().getPackageName()));
        Animation animation = AnimationUtils.loadAnimation(this,getApplication().getResources()
                        .getIdentifier("anim_rotate","anim",getApplication().getPackageName()));
        LinearInterpolator lin = new LinearInterpolator();
        animation.setInterpolator(lin);
        iv_dialogLoading.setAnimation(animation);
        //得到屏幕的宽
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        mDialog.setContentView(v,new LinearLayout.LayoutParams(width/3,width/3));
        mDialog.setCancelable(false);
        if (!mDialog.isShowing()){
            mDialog.show();
        }
    }
}
