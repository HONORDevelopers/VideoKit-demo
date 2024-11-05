/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.videokit.videoenhance;

import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hihonor.mcs.media.video.HdrLightnessClient;
import com.hihonor.mcs.media.video.HnVideoClient;
import com.hihonor.mcs.media.video.IVideoServiceCallback;
import com.hihonor.mcs.media.video.common.StatusCode;
import com.hihonor.videokit.BaseActivity;
import com.hihonor.videokit.R;

import java.io.IOException;

public class EHdrSurfaceActivity extends BaseActivity implements SurfaceHolder.Callback, IVideoServiceCallback {
    private static final String TAG = "EHdrSurfaceActivity";

    private static final String VIDEO_PATH = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/test_HDR.mp4";

    protected SurfaceHolder mSurfaceHolder = null;

    private SurfaceView mSurfaceView = null;

    private MediaPlayer mMediaPlayer;

    private HnVideoClient mHnVideoClient = null;

    private HdrLightnessClient mHdrLightnessClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ehdr_surface);
        initView();
        initData();
    }

    private void initView() {
        mSurfaceView = findViewById(R.id.surface_display);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        try {
            initMediaPlayer();
        } catch (IOException e) {
            Log.e(TAG, "surfaceCreated: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    @Override
    public void onResult(int result) {
        Log.i(TAG, "result = " + result);
        switch (result) {
            case StatusCode.VIDEO_SERVICE_SUCCESS: // 初始化video-kit实例成功
                initHnBrightnessClient();
                break;
            case StatusCode.VIDEO_SERVICE_HDR_FEATURE_SUCCESS: // 创建VideoKit亮度监听特性实例成功
                enableHdr(null);
                registerBrightnessListener();
                break;
            case StatusCode.VIDEO_NO_PERMISSION:
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
                break;
            case StatusCode.VIDEO_CONTEXT_NULL:
                // 构造方法context参数为空, APP需要确保context不能为空，自行处理
                break;
            case StatusCode.VIDEO_SERVICE_DISCONNECTED:
                // 服务断开, APP自行处理
                break;
            case StatusCode.VIDEO_INVALID_CALL:
                // 非法无效调用，当client被destroy或者没有进行初始化，会返回此类错误码
            default:
                break;
        }
    }

    /**
     * 初始化化对象
     */
    private void initData() {
        if (mHnVideoClient != null) {
            return;
        }
        mHnVideoClient = new HnVideoClient(getApplicationContext(), this);
        mHnVideoClient.initialize(); // 初始化绑定服务并设置服务状态回调，这里存在一些耗时，所以采用异步方式（耗时在100-200ms之间）
    }

    // 判断当前设备是否支持HDR亮度监听特性，并创建特性客户端
    private void initHnBrightnessClient() {
        Log.i(TAG, "initHnBrightnessClient");
        if (mHnVideoClient != null
            && mHnVideoClient.isServiceSupported(HnVideoClient.ServiceType.HNVIDEO_SERVICE_LIGHTNESS_INFO)) {
            mHdrLightnessClient =
                mHnVideoClient.createService(HnVideoClient.ServiceType.HNVIDEO_SERVICE_LIGHTNESS_INFO);
        } else {
            Log.e(TAG, "failed");
            Toast.makeText(this, "当前设备不支持 HDR 视频增强能力", Toast.LENGTH_SHORT).show();
            // mHnVideoClient 未初始化或者设备不支持该特性
        }
    }

    // 注册亮度监听
    private void registerBrightnessListener() {
        if (mHdrLightnessClient != null) {
            mHdrLightnessClient.registerLightnessChangedListener(light -> Log.i(TAG, "Light value = " + light));
        }
    }

    // 注销亮度监听
    private void unregisterBrightnessListener() {
        if (mHdrLightnessClient != null) {
            mHdrLightnessClient.unregisterLightnessChangedListener();
        }
    }

    // 开启HDR模式
    public void enableHdr(View view) {
        if (mHdrLightnessClient != null) {
            // 开启 SurfaceView 的 HDR 增强能力
            HdrLightnessClient.Options options = new HdrLightnessClient.Options();
            // false表示非悬浮窗
            options.setWindowFloating(false);
            boolean result = mHdrLightnessClient.enableSurfaceHdrMode(mSurfaceView, null);

            Log.i(TAG, "enable SurfaceHdrMode result = " + result);
            Toast.makeText(this, "已开启 HDR 增强能力", Toast.LENGTH_SHORT).show();
        }
    }

    // 关闭HDR模式
    public void disableHdr(View view) {
        if (mHdrLightnessClient != null) {
            Log.i(TAG, "disableSurfaceHdrMode");

            // 关闭 SurfaceView 的 HDR 增强能力
            mHdrLightnessClient.disableSurfaceHdrMode(mSurfaceView);
            Toast.makeText(this, "已关闭 HDR 增强能力", Toast.LENGTH_SHORT).show();
        }
    }

    // 判断是否支持HDR VIVID硬件能力
    public void isHdrVividSupported(View view) {
        if (mHnVideoClient != null && mHnVideoClient.isHdrVividSupported()) {
            Toast.makeText(this, "当前设备支持 HDR VIVID 硬解能力", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "当前设备不支持 HDR VIVID 硬解能力", Toast.LENGTH_SHORT).show();
        }
    }

    private void initMediaPlayer() throws IOException {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.reset();
        mMediaPlayer.setOnVideoSizeChangedListener((mp, width, height) -> changeVideoSize());
        // 设置播放的视频数据源
        try {
            mMediaPlayer.setVolume(0, 0);
            mMediaPlayer.setDataSource(VIDEO_PATH);
            mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            mMediaPlayer.setOnCompletionListener(mp -> {
                if (mMediaPlayer == null) {
                    return;
                }
                mMediaPlayer.start();
            });
        } catch (Exception e) {
            Log.e(TAG, "initMediaPlayer: " + e.getMessage());
            Toast.makeText(this, "视频播放异常：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void changeVideoSize() {
        int videoWidth = mMediaPlayer.getVideoWidth();
        int videoHeight = mMediaPlayer.getVideoHeight();
        int surfaceWidth = mSurfaceView.getWidth();
        int surfaceHeight = mSurfaceView.getHeight();

        // 根据视频尺寸去计算->视频可以在surfaceView中放大的最大倍数。
        float max;
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            // 竖屏模式下按视频宽度计算放大倍数值
            max = Math.max((float) videoWidth / (float) surfaceWidth, (float) videoHeight / (float) surfaceHeight);
        } else {
            // 横屏模式下按视频高度计算放大倍数值
            max = Math.max(((float) videoWidth / (float) surfaceHeight), (float) videoHeight / (float) surfaceWidth);
        }

        // 视频宽高分别/最大倍数值 计算出放大后的视频尺寸
        videoWidth = (int) Math.ceil((float) videoWidth / max);
        videoHeight = (int) Math.ceil((float) videoHeight / max);

        // 无法直接设置视频尺寸，将计算出的视频尺寸设置到surfaceView 让视频自动填充。
        mSurfaceView.setLayoutParams(new RelativeLayout.LayoutParams(videoWidth, videoHeight));
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 退到后台需要调用 HdrLightnessClient#disableTextureHdrMode 接口
        disableHdr(null);
        // 注销亮度监听
        unregisterBrightnessListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroy();
    }

    // 销毁并注销VideoKit
    protected void destroy() {
        if (mHdrLightnessClient != null) {
            mHdrLightnessClient.unregisterLightnessChangedListener();
            mHdrLightnessClient.destroy();
            mHdrLightnessClient = null;
        }
        if (mHnVideoClient != null) {
            mHnVideoClient.destroy();
            mHnVideoClient = null;
        }
    }
}