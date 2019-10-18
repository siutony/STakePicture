package com.siu.stakepicture;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.siu.stakepicture.camera.CameraManager;

import java.util.ArrayList;
import java.util.List;


public class TakePhoneAct extends Activity implements SurfaceHolder.Callback {
    private static final int REQ_PERMISSION = 1;

    public CameraManager mCameraManager;
    private AlertDialog mDialog;
    private SurfaceView mSurfaceView;
    private ImageView mImgResult;
    private ImageView mImgOk;
    private ImageView mImgCancel;
    private ImageView mImgChange;
    private View mLayoutOpe;
    private View mShoot;
    private boolean mIsFront;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_camera_test);
        mSurfaceView = findViewById(R.id.surfaceView);

        mImgResult = findViewById(R.id.imgResult);
        mImgCancel = findViewById(R.id.imgCancel);
        mImgOk = findViewById(R.id.imgOk);
        mLayoutOpe = findViewById(R.id.layoutOpe);
        mImgChange = findViewById(R.id.imgChange);

        mSurfaceView.getHolder().addCallback(this);
        mCameraManager = new CameraManager(this);

        mShoot = findViewById(R.id.shoot);
        mShoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraManager.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes, Camera camera) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        Camera.CameraInfo info = mCameraManager.getCameraInfo();
                        bitmap = BitmapUtil.rotateAndMirrorBitmap(bitmap, info.orientation, info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
                        mImgResult.setImageBitmap(bitmap);
                        mImgResult.setVisibility(View.VISIBLE);
                        mLayoutOpe.setVisibility(View.VISIBLE);
                        mShoot.setVisibility(View.GONE);
                        mImgChange.setVisibility(View.GONE);
                        mSurfaceView.setVisibility(View.GONE);
                        mCameraManager.stopPreview();
                    }
                });
            }
        });
        mImgCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mImgResult.setVisibility(View.GONE);
                mLayoutOpe.setVisibility(View.GONE);
                mShoot.setVisibility(View.VISIBLE);
                mImgChange.setVisibility(View.VISIBLE);
                mSurfaceView.setVisibility(View.VISIBLE);
                mCameraManager.startPreview();
            }
        });
        mImgOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mImgChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraManager.closeDriver();
                mIsFront = !mIsFront;
                openCamera();
            }
        });
    }

    private void openCamera() {
        try {
            //设置前置或后置摄像头
            mCameraManager.setManualCameraId(mIsFront ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK);
            //打开摄像头
            mCameraManager.openDriver(mSurfaceView.getHolder());
            //开始预览
            mCameraManager.startPreview();
        } catch (Exception ioe) {
            //捕获异常,提示并推出
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            mDialog = builder.setMessage("打开摄像头失败，请退出重试").setNegativeButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }).create();
            mDialog.setCancelable(false);
            mDialog.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                boolean camera = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
                if (!camera) { // 判断是否勾选了不再提醒，如果有勾选，提权限用途，点击确定跳转到App设置页面
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    mDialog = builder.setMessage("请在设置-应用-xxx中，设置运行使用摄像头权限").setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            goIntentSetting();
                        }
                    }).create();
                    mDialog.setCancelable(false);
                    mDialog.show();
                } else { //拒绝了权限
                    finish();
                }
            }
        }
    }

    /**
     * 应用设置页面
     */
    private void goIntentSetting() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        try {
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] deniList = checkPermissionsGranted(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA});
            if (deniList != null && deniList.length > 0) { //未授权
                requestPermissions(deniList, REQ_PERMISSION);
            } else {
                openCamera();
            }
        } else {
            openCamera();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCameraManager.closeDriver();
    }

    @TargetApi(23)
    public String[] checkPermissionsGranted(String[] permissions) {
        List<String> deniList = new ArrayList<>();

        // 遍历每一个申请的权限，把没有通过的权限放在集合中
        for (String permission : permissions) {
            if (checkSelfPermission(permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                deniList.add(permission);
            }
        }
        return deniList.toArray(new String[deniList.size()]);
    }

    @Override
    protected void onDestroy() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        super.onDestroy();
    }
}
