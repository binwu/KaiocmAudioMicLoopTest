package cn.kaicom.kaiocmaudiomiclooptest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String FILE_NAME = "MainMicRecord";
    private static final int SAMPLE_RATE = 44100;//Hz，采样频率
    private static final double FREQUENCY = 500; //Hz，标准频率（这里分析的是500Hz）
    private static final double RESOLUTION = 10; //Hz，误差
    private static final long RECORD_TIME = 2000;
    private File mSampleFile;
    private int bufferSize = 0;
    private AudioRecord mAudioRecord;

    final String TEST_NAME = "testSetStereoVolumeMax";
    final int TEST_SR = 22050;
    final int TEST_CONF = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
    final int TEST_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    final int TEST_MODE = AudioTrack.MODE_STREAM;
    final int TEST_STREAM_TYPE = AudioManager.STREAM_MUSIC;
    volatile AudioTrack track;
    Handler handler =  new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 0)
            new Thread(new AudioRecordThread()).start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        startRecord();
    }

    private void startRecord() {

        //为了方便，这里只录制单声道
        //如果是双声道，得到的数据是一左一右，注意数据的保存和处理
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,//MediaRecorder.AudioSource.VOICE_CALL  MIC
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        int minBufferSize = AudioTrack.getMinBufferSize(TEST_SR, AudioFormat.CHANNEL_IN_MONO, TEST_FORMAT);
        track = new AudioTrack(AudioManager.STREAM_VOICE_CALL, TEST_SR, TEST_CONF,
                TEST_FORMAT,   bufferSize, AudioTrack.MODE_STREAM);//AudioManager.STREAM_VOICE_CALL   STREAM_MUSIC
//        track.setVolume(150f);//设置音量
        track.play();
//        new Thread(new AudioRecordThread()).start();

        handler.sendEmptyMessage(0);
    }

    private class AudioRecordThread implements Runnable {


        @Override
        public void run() {
            //将录音数据写入文件
            short[] audiodata = new short[bufferSize  ];
            mAudioRecord.startRecording();
            synchronized (track) {
                try {
//                fos = new DataOutputStream(new FileOutputStream(mSampleFile));
                    int readSize;
                    while (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        readSize = mAudioRecord.read(audiodata, 0, audiodata.length);
                        if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
                            short[] data = new short[readSize];
                            System.arraycopy(audiodata, 0, data, 0, readSize);
//                        for (int i = 0; i < readSize; i++) {
//                            fos.writeShort(audiodata[i]);
//                            fos.flush();
//                        }
                            track.write(data, 0, readSize);
                            track.flush();

                        }
                    }
                } finally {
                    if (track != null) {

//                        fos.close();
                        track.release();

                    }

                    //在这里release
                    mAudioRecord.release();
                    mAudioRecord = null;
                }
            }
        }
    }

    ;

    //在这里stop的时候先不要release
    private void stopRecording() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopRecording();


    }
}
