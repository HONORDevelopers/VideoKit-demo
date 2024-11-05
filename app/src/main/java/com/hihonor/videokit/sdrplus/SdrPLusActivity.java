/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */
package com.hihonor.videokit.sdrplus;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import androidx.annotation.NonNull;
import com.hihonor.mcs.media.video.HnVideoClient;
import com.hihonor.mcs.media.video.IVideoServiceCallback;
import com.hihonor.mcs.media.video.OnSdrPlusStateChangedListener;
import com.hihonor.mcs.media.video.SdrPlusClient;
import com.hihonor.mcs.media.video.common.StatusCode;
import com.hihonor.videokit.BaseActivity;
import com.hihonor.videokit.R;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SdrPLusActivity extends BaseActivity implements SurfaceHolder.Callback, View.OnClickListener,
        IVideoServiceCallback {

    private static final String TAG = "SdrPLusActivity";

    private final String mPath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/test.mp4";

    private MediaFormat mVideoFormat;

    private MediaCodec mDecoder;

    private MediaExtractor mMediaExtractor;

    private SurfaceView mSurfaceView;

    ByteBuffer[] codecOutputBuffers;

    private HnVideoClient mHnVideoClient = null;

    private SdrPlusClient mHnSdrPlusClient = null;

    // 0:configured 1:flushed 2:running 3:EOS 4uninitialized 4:stopped 5:release 6:error
    private int mCodecStat;

    float inputMaxRatio;

    float inputMinRatio;

    private float mSdrRatio = 1.5F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdr_plus);
        initView();

        mHnVideoClient = new HnVideoClient(getApplicationContext(), this);
        mHnVideoClient.initialize(); // 初始化绑定服务并设置服务状态回调，这里存在一些耗时，所以采用异步方式（耗时在100-200ms之间）
    }

    public void initHnSdrPlusClient() {
        Log.i(TAG, "initHnSdrPlusClient");
        if (mHnVideoClient != null
                && mHnVideoClient.isServiceSupported(HnVideoClient.ServiceType.HNVIDEO_SERVICE_SDRPLUS_INFO)) {
            mHnSdrPlusClient = mHnVideoClient.createService(HnVideoClient.ServiceType.HNVIDEO_SERVICE_SDRPLUS_INFO);
            Log.i(TAG, "initHnSdrPlusClient end");
        } else {
            Log.e(TAG, "failed");
            // TODO: mHnVideoClient 未初始化或者设备不支持该特性
        }
    }

    // 获取当前状态
    public void getCurrentSdrPlusState() {
        Log.i(TAG, "getCurrentSdrPlusState");
        if (mHnSdrPlusClient != null) {
            int state = mHnSdrPlusClient.getCurrentSdrPlusState();
            Log.i(TAG, "getCurrentSdrPlusState = " + state);
        }
    }

    public void getOutFormatRatio() {
        Log.i(TAG, "getOutFormatRatio");
        float ratio = 0.0f;
        if(mDecoder != null){
            MediaFormat format = mDecoder.getOutputFormat();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ratio = format.getFloat("lightness-ratio", 0.0f);
            }
            Log.i(TAG, "getOutFormatRatio " + ratio);
        }
    }

    // 给图层打标记
    public void enableSdrPlus() {
        Log.i(TAG, "enableSdrPlus");
        if (mHnSdrPlusClient != null) {
            // 先取消后再进行打标记
            mHnSdrPlusClient.setSdrPlusFlag(mSurfaceView, mHnSdrPlusClient.FLAG_DISABLE_SURFACE);
            mHnSdrPlusClient.setSdrPlusFlag(mSurfaceView, mHnSdrPlusClient.FLAG_ENABLE_SURFACE);
        }
    }

    // 给图层取消标记
    public void disableSurface() {
        Log.i(TAG, "disableSurface");
        if (mHnSdrPlusClient != null) {
            mHnSdrPlusClient.setSdrPlusFlag(mSurfaceView, mHnSdrPlusClient.FLAG_DISABLE_SURFACE);
            Bundle params = new Bundle();
            if(mDecoder != null){
                params.putFloat("lightness-ratio", -1f);
                mDecoder.setParameters(params);
                getOutFormatRatio();
            } else {
                Log.i(TAG, "mDecoder is null");
            }
        }
    }

    // 注册状态变化监听
    void registerSdrPlusStatusListener() {
        if (mHnSdrPlusClient != null) {
            mHnSdrPlusClient.registerSdrPlusStateChangedListener(new OnSdrPlusStateChangedListener() {
                @Override
                public void onSdrPlusStateChanged(int state) {
                    Log.i(TAG, "OnSdrPlusStateChanged = " + state );
                }
            });
        }
    }

    protected void initView() {
        mSurfaceView = findViewById(R.id.sv_surfaceview);
        mSurfaceView.setKeepScreenOn(true);
        mSurfaceView.getHolder().addCallback(this);
        findViewById(R.id.btn_video_play).setOnClickListener(this);
        findViewById(R.id.btn_open_sdr).setOnClickListener(this);
        findViewById(R.id.btn_close_sdr).setOnClickListener(this);
        findViewById(R.id.btn_15).setOnClickListener(this);
        findViewById(R.id.btn_20).setOnClickListener(this);
        findViewById(R.id.btn_25).setOnClickListener(this);
    }

    public void initCodec() {
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int trackCount = mMediaExtractor.getTrackCount();
        int trackID = -1;
        String extractMimeType = "video/";
        for (int i = 0;i < trackCount; i++) {
            mVideoFormat = mMediaExtractor.getTrackFormat(i);
            if (mVideoFormat.getString(MediaFormat.KEY_MIME).startsWith(extractMimeType)) {
                trackID = i;
                break;
            }
        }
        // 媒体文件中存在视频轨道
        if (trackID != -1){
            mMediaExtractor.selectTrack(trackID);
            Log.i(TAG, "trackID = " + trackID);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void doSomeWork(Surface surface) throws IOException {
        initCodec();
        Log.d(TAG, String.format("TRACKS #: %d", mMediaExtractor.getTrackCount()));
        String mime = mVideoFormat.getString(MediaFormat.KEY_MIME);
        Log.d(TAG, String.format("MIME TYPE: %s", mime));

        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder.release();
            mDecoder = null;
        }
        mDecoder = MediaCodec.createDecoderByType(mime);
        PlayClock clock = new PlayClock();
        clock.setSpeed(1F);
        clock.start();

        mDecoder.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                //Retrieve the set of input buffers. Call this after start() returns. After calling this method, any ByteBuffers previously returned by an earlier call to this method MUST no longer be used.
                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                int sampleSize = mMediaExtractor.readSampleData(inputBuffer, 0);
                if (sampleSize < 0) {
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    mCodecStat = 3;
                } else {
                    codec.queueInputBuffer(index, 0, sampleSize, mMediaExtractor.getSampleTime(), 0);
                    mMediaExtractor.advance();
                    mCodecStat = 2;
                }
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                switch (index) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        codecOutputBuffers = codec.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:

                        break;
                    default:
                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        // 控制播放速度
                        while (info.presentationTimeUs / 1000 > clock.getCurrentPlayTime()) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        if (surface.isValid() && mCodecStat != 6 && mCodecStat != 5) {
                            codec.releaseOutputBuffer(index, true);
                        }
                        break;
                }
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                mCodecStat = 6;
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

            }
        });

        if (mHnSdrPlusClient != null) {
            inputMaxRatio = mHnSdrPlusClient.getMaxLightnessRatio();
            inputMinRatio = mHnSdrPlusClient.getMinLightnessRatio();
            Log.i(TAG, "inputMaxRatio = " + inputMaxRatio + ", inputMinRatio = " + inputMinRatio);
            // 对视频画质增强系数的设置
            mVideoFormat.setFloat("lightness-ratio", mSdrRatio);

        } else {
            Log.i(TAG, "mHnSdrPlusClient is null ");
        }

        mDecoder.configure(mVideoFormat, surface, null, 0);
        getOutFormatRatio();
        mDecoder.start();
        mCodecStat = 2;

    }

    boolean isSurfaceCreated = false;
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        isSurfaceCreated = true;
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_video_play:
                try {
                    doSomeWork(mSurfaceView.getHolder().getSurface());
                    getCurrentSdrPlusState();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case R.id.btn_open_sdr:
                if (isSurfaceCreated) {
                    enableSdrPlus();
                }
                break;
            case R.id.btn_close_sdr:
                if (isSurfaceCreated) {
                    disableSurface();
                }
                break;
            case R.id.btn_15:
                mSdrRatio = 1.5f;
                break;
            case R.id.btn_20:
                mSdrRatio = 2.0f;
                break;
            case R.id.btn_25:
                mSdrRatio = 2.5f;
                break;
            default:
                break;
        }
    }

    @Override
    public void onResult(int result) {
        Log.i(TAG, "result = " + result);
        switch (result) {
            case StatusCode.VIDEO_SERVICE_SUCCESS: // 初始化video-kit实例成功
                initHnSdrPlusClient();
                break;
            case StatusCode.VIDEO_SERVICE_SDRPLUS_FEATURE_SUCCESS: // 创建VideoKit 超动态显示特性实例成功
                registerSdrPlusStatusListener();
                break;
            case StatusCode.VIDEO_CONTEXT_NULL:
                // TODO: 构造方法context参数为空, APP需要确保context不能为空，自行处理
                break;
            case StatusCode.VIDEO_SERVICE_DISCONNECTED:
                // TODO: 服务断开, APP自行处理
                break;
            case StatusCode.VIDEO_INVALID_CALL:
                // TODO: 非法无效调用，当client被destroy或者没有进行初始化，会返回此类错误码
            default:
                break;
        }
    }

    class PlayClock {
        private long start;
        private float speed = 1.0f;

        public void setSpeed(float speed) {
            this.speed = speed;
        }

        public void start() {
            start = SystemClock.elapsedRealtime();
        }

        public long getCurrentPlayTime() {
            return (long) ((float) (SystemClock.elapsedRealtime() - start) * speed);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder.release();
            mDecoder = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroy();
    }

    // 销毁并注销VideoKit
    protected void destroy() {
        if (mHnSdrPlusClient != null) {
            mHnSdrPlusClient.unregisterSdrPlusStateChangedListener();
            mHnSdrPlusClient.destroy();
            mHnSdrPlusClient = null;
        }
        if (mHnVideoClient != null) {
            mHnVideoClient.destroy();
            mHnVideoClient = null;
        }
    }
}