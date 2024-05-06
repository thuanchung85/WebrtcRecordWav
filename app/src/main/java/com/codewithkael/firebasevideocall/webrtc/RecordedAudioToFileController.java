package com.codewithkael.firebasevideocall.webrtc;
import static com.codewithkael.firebasevideocall.webrtc.PCmTowav.PCMToWAV;

import android.media.AudioFormat;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule.SamplesReadyCallback;
public class RecordedAudioToFileController implements SamplesReadyCallback {
    private static final String TAG = "RecordedAudioToFile";
    private static final long MAX_FILE_SIZE_IN_BYTES = 58348800L;

    private final Object lock = new Object();
    private final ExecutorService executor;
    @Nullable private OutputStream rawAudioFileOutputStream;
    private boolean isRunning;
    private long fileSizeInBytes;
    File outputFilePCM;
    File outputFileWav;
    public RecordedAudioToFileController(ExecutorService executor) {

        this.executor = executor;

    }

    /**
     * Should be called on the same executor thread as the one provided at
     * construction.
     */
    public boolean start() {


        Log.d("CHUNG", "RecordedAudioToFileController ACTIVED");

        String OUTPUT_FILE = "CHUNGrecorded_audio_NEW.pcm";
        String OUTPUT_FILE_WAV = "CHUNGrecorded_audio_NEW.wav";
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YourAppName");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        outputFilePCM = new File(directory, OUTPUT_FILE);

        outputFileWav = new File(directory, OUTPUT_FILE_WAV);
        try {
            Log.d("CHUNG-", String.format("CHUNG-  ProcessVoice outputFile.createNewFile()"));
            outputFilePCM.createNewFile();
            outputFileWav.createNewFile();

            //start timer
            startTimerTick();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(outputFilePCM);
        System.out.println(outputFileWav);

        Log.d("CHUNG", "CHUNG AUDIO RECORD start");
        if (!isExternalStorageWritable()) {
            Log.e(TAG, "Writing to external media is not possible");
            return false;
        }
        synchronized (lock) {
            isRunning = true;
        }
        return true;


    }

    /**
     * Should be called on the same executor thread as the one provided at
     * construction.
     */
    public void stop() {
        stopTimerTIck();
        Log.d(TAG, "stop");
        synchronized (lock) {
            isRunning = false;
            if (rawAudioFileOutputStream != null) {
                try {
                    rawAudioFileOutputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close file with saved input audio: " + e);
                }
                rawAudioFileOutputStream = null;
            }
            fileSizeInBytes = 0;
        }
/*
        //convert PCM to wav and clear pcm file
        File read = new File(outputFilePCM.getPath());
        File out = new File(outputFileWav.getPath());
        try {

            PCMToWAV(read, out, getChannelCount, getSampleRate, 16);
            Log.e("CHUNG", "PCMToWAV RUN OK " + getChannelCount + ": " + getSampleRate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

 */
    }

    // Checks if external storage is available for read and write.
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    // Utilizes audio parameters to create a file name which contains sufficient
    // information so that the file can be played using an external file player.
    // Example: /sdcard/recorded_audio_16bits_48000Hz_mono.pcm.
    private void openRawAudioOutputFile(int sampleRate, int channelCount) {


        /*
        final String fileName = Environment.getExternalStorageDirectory().getPath() + File.separator
                + "recorded_audio_16bits_" + String.valueOf(sampleRate) + "Hz"
                + ((channelCount == 1) ? "_mono" : "_stereo") + ".pcm";*/
        //final File outputFile = new File(fileName);
        final File outputFile = outputFilePCM;

        try {
            getChannelCount = channelCount;
            getSampleRate = sampleRate;
            rawAudioFileOutputStream = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to open audio output file: " + e.getMessage());
        }
        Log.d("CHUNG", "CHUNG CHECK Opened file for recording: " + outputFile.getAbsolutePath());
    }

    // Called when new audio samples are ready.
    int getSampleRate ;
    int getChannelCount;
    boolean isHearVoice;

    private Timer timerTick;
    int chayTick =0;


    public void startTimerTick() {
         TimerTask timerTask = new TimerTask() {

            @Override
            public void run() {

                if(chayTick > 1) {
                    chayTick = 0;
                    Log.w("CHUNG", "CHUNG CHECK STOP VOICE NOW: " + chayTick);
                    //ghi ra file wav
                    //convert PCM to wav and clear pcm file
                    File read = new File(outputFilePCM.getPath());
                    read.deleteOnExit();
                    Log.w("CHUNG", "CHUNG CHECK CLEAR outputFilePCM " + outputFilePCM.exists());
                    try {
                        read.createNewFile();

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    File out = new File(outputFileWav.getPath());
                    out.deleteOnExit();
                    Log.w("CHUNG", "CHUNG CHECK CLEAR outputFileWav " + outputFileWav.exists());
                    try {
                        out.createNewFile();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        //ghi file pcm thanh wav
                        PCMToWAV(read, out, getChannelCount, getSampleRate, 16);
                        Log.w("CHUNG", "CHUNG CHECK GHI FILE WAV " + getChannelCount + ": " + getSampleRate);

                        //xoá đóng pcm củ để ghi lại stream mới đè lên, mục tiêu là lấy câu nói cuối cùng của user.
                        if(rawAudioFileOutputStream != null) {
                           rawAudioFileOutputStream.close();
                           rawAudioFileOutputStream = new FileOutputStream(outputFilePCM);
                           fileSizeInBytes = 0;
                       }


                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                /*
                //chổ này ta send data đi va xoa file wav làm lại
                //????
                //thử copy ra file wav riêng
                File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YourAppName");
                if (directory.exists()) {
                    String OUTPUT_FILE = "CHUNGrecorded_audio_NEW.wav";
                    File audioFile = new File(directory, OUTPUT_FILE);
                    System.out.println(audioFile);
                    if (audioFile.exists()) {
                         File copyF = new File( new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YourAppName"),
                                "SentenceCHUNGrecorded_audio_tempCopy.wav");
                        try {
                            Log.e("CHUNG", "CHUNG CHECK COPY --- COPY NOW");
                            copyToNewFileWav(audioFile, copyF);


                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }
*/


                }
                chayTick ++;
                Log.d("CHUNG", "CHUNG CHECK timer: " + chayTick);

            }
        };

        if(timerTick != null) {
            return;
        }
        timerTick = new Timer();

        timerTick.schedule(timerTask, 0, 1000);
    }

    public void stopTimerTIck() {
        timerTick.cancel();
        timerTick = null;
    }

    //hàm này loop liên tục
    @Override
    public void onWebRtcAudioRecordSamplesReady(JavaAudioDeviceModule.AudioSamples samples) {
        Log.d("CHUNG", "CHUNG  onWebRtcAudioRecordSamplesReady: " +samples);

        // The native audio layer on Android should use 16-bit PCM format.
        if (samples.getAudioFormat() != AudioFormat.ENCODING_PCM_16BIT) {
            Log.e(TAG, "Invalid audio format");
            return;
        }

        synchronized (lock) {
            // Abort early if stop() has been called.
            if (!isRunning) {
                return;
            }
            // Open a new file for the first callback only since it allows us to add audio parameters to
            // the file name.
            if (rawAudioFileOutputStream == null) {
                openRawAudioOutputFile(samples.getSampleRate(), samples.getChannelCount());
                fileSizeInBytes = 0;


            }
        }
        // Append the recorded 16-bit audio samples to the open output file.
        executor.execute(() -> {
            if (rawAudioFileOutputStream != null) {
                try {
                    // Set a limit on max file size. 58348800 bytes corresponds to
                    // approximately 10 minutes of recording in mono at 48kHz.
                    if (fileSizeInBytes < MAX_FILE_SIZE_IN_BYTES) {
                        // Writes samples.getData().length bytes to output stream.

                        rawAudioFileOutputStream.write(samples.getData());
                        fileSizeInBytes += samples.getData().length;
                        Log.e("CHUNG", " to write audio to file: " + fileSizeInBytes + " byte");

                        //cần 1 cơ chế nhận diện khi user ngưng nói thì gởi data củ đi,xoá data củ ghi file lại từ đầu
                        byte[] mbuffer = samples.getData();
                        int size = samples.getData().length;
                        //Log.e("CHUNG", "CHUNG CHECK isHearVoice: " + samples.getSampleRate());
                        //Log.e("CHUNG", "CHUNG CHECK isHearVoice: " + Arrays.toString(mbuffer));
                        //Log.e("CHUNG", "CHUNG CHECK isHearVoice: " + size);
                         isHearVoice = isHearingVoice(mbuffer, size);
                        if(isHearVoice == true) {
                            Log.e("CHUNG", "CHUNG CHECK isHearVoice: " + isHearVoice);
                            chayTick --;
                            if(chayTick < 0) chayTick = 0;
                        }
                        //ta cần 1 bộ timer để bật ngược lại isHearVoice = false để chuyển qua state không nghe thấy voice gì sau khi nghe 1 loạt voice


                    }
                } catch (IOException e) {
                    Log.e(TAG, "Failed to write audio to file: " + e.getMessage());
                }
            }
        });
    }

    ///========func hellper==//
    private boolean isHearingVoice(byte[] buffer, int size) {
        //for duyệt qua cac buffer nằm ở vi trí chẵn
        for (int i = 0; i < size - 1; i += 2) {
            // The buffer has LINEAR16 in little endian.
            //Note that the default behavior of byte-to-int conversion is to preserve the sign of the value (remember byte is a signed type in Java). So for instance:
            //
            //byte b1 = -100;
            //int i1 = b1;
            //System.out.println(i1); // -100
            int s = buffer[i + 1];
            if (s < 0) s *= -1;
            //DỊCH 8 BIT QUA TRÁI NGHỈA LÀ S SẼ MŨ 8 LẦN LÊN
            s <<= 8;
            //đem s + với giá trị phần tữ trước đó, vi trí lẽ
            s += Math.abs(buffer[i]);
            //KHAI BÁO NGƯỠNG ÂM THANH, mà nếu vượt nguonng này thi coi như là có data voice thường là 2000
            //int amplitudeThreshold = global.getAmplitudeThreshold();
            int amplitudeThreshold = 350;
            //nếu s vượt ngưỡng âm thanh 280 thì coi như có tiếng con người nói vào buffter

            if (s > amplitudeThreshold) {
                Log.e("CHUNG", "CHUNG CHECK amplitudeThreshold: " + s);
                return true;
            }
        }
        return false;
    }


    public  void copyToNewFileWav(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();


            }
        } finally {
            in.close();


        }
    }

}