package utils;


import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.*;

import javax.sound.sampled.*;
import javax.xml.bind.ValidationEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author YanYuHang
 * @create 2019-03-23-9:09
 */
public class Video {
    /**
     * @param WEBCAM_DEVICE_INDEX 视频设备  本机默认是0
     * @param AUDIO_DEVICE_INDEX  音频设备  本机默认是4
     * @param outputFile          文件保存路径或是文件名（D://Demo/Test.mp4）
     * @param captureWidth        摄像头的宽
     * @param captureHeight       摄像头的高
     * @param FRAME_RATE          视频帧率（25 代表 每秒25张图片）
     * @throws org.bytedeco.javacv.FrameGrabber.Exception
     */
    public static void recordWebcamAndMicrophone(int WEBCAM_DEVICE_INDEX, final int AUDIO_DEVICE_INDEX, String outputFile,
                                                 int captureWidth, int captureHeight, final int FRAME_RATE) throws org.bytedeco.javacv.FrameGrabber.Exception {
        //开始时间
        long startTime = 0;
        //视频总长
        long videoTS = 0;
        //开启摄像头设备
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(WEBCAM_DEVICE_INDEX);
        //设置摄像头的宽
        grabber.setImageWidth(captureWidth);
        //设置摄像头的高
        grabber.setImageHeight(captureHeight);
        System.out.println("开始抓取摄像头。。。。。。。。");
        //设置摄像头的开启状态
        int isTrue = 0;
        //开始获取摄像头数据
        try {
            grabber.start();
            //更新摄像头状态
            isTrue += 1;
        } catch (FrameGrabber.Exception e1) {
            //启动报错后  如果 grabber不为空  就重启
            try {
                if (grabber != null) {
                    grabber.restart();
                    isTrue += 1;
                }
            } catch (FrameGrabber.Exception e2) {
                //如果重启也报错 那么 将此进程结束
                isTrue -= 1;
                try {
                    grabber.stop();
                } catch (FrameGrabber.Exception e3) {
                    isTrue -= 1;
                }
            }
        }

        //判断摄像头状态
        if (isTrue < 0) {
            System.err.println("摄像头开启失败，重启也失败");
            return;
        } else if (isTrue < 1) {
            System.err.println("摄像头启动失败");
            return;
        } else if (isTrue == 1) {
            System.out.println("摄像头开启成功");
        }
        /*******************************************以下为音频操作*******************************************************/
        /**
         * FFmpegFrameRecorder(String fileName,int imageWidth,int imageHeight，int audioChannels ())
         * fileName   文件名  RTMP  流媒体服务器路径
         * audioChannels    0（无音频）    1（单声道） 2（立体声）
         */
        final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, captureWidth, captureHeight, 2);
        recorder.setInterleaved(true);

        /*降低延迟
         * 用于降低延迟
         * -tune zerolatency -b 900k -f
         * */
        recorder.setVideoOption("tune", "zerolatency");
        /**
         * 视频质量
         * ultrafast  终极快
         * superfast  超级快
         * veryfast   非常快
         * faster     很快
         * fast       快
         * medium     中等
         * slow       慢
         * slower     很慢
         * veryslow    非常慢
         *
         */
        recorder.setVideoOption("preset", "ultrafast");
        //保证视频质量不低于25
        recorder.setVideoOption("crf", "25");
        // 2000 kb/s, 720P视频的合理比特率范围
        recorder.setVideoBitrate(2000000);
        // h264编/解码器
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        // 封装格式flv
        recorder.setFormat("flv");
        // 视频帧率(保证视频质量的情况下最低25，低于25会出现闪屏)
        recorder.setFrameRate(FRAME_RATE);
        // 关键帧间隔，一般与帧率相同或者是视频帧率的两倍
        recorder.setGopSize(FRAME_RATE * 2);
        // 不可变(固定)音频比特率
        recorder.setAudioOption("crf", "0");
        // 最高质量
        recorder.setAudioQuality(0);
        // 音频比特率
        recorder.setAudioBitrate(192000);
        // 音频采样率
        recorder.setSampleRate(44100);
        // 双通道(立体声)
        recorder.setAudioChannels(2);
        // 音频编/解码器
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        System.out.println("开始录制...");
        try {
            recorder.start();
        } catch (org.bytedeco.javacv.FrameRecorder.Exception e2) {
            if (recorder != null) {
                System.out.println("关闭失败，尝试重启");
                try {
                    recorder.stop();
                    recorder.start();
                } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                    try {
                        System.out.println("开启失败，关闭录制");
                        recorder.stop();
                        return;
                    } catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {
                        return;
                    }
                }
            }

        }
        // 音频捕获
        new Thread(new Runnable() {
            @Override
            public void run() {
                /**
                 * 设置音频编码器 最好是系统支持的格式，否则getLine() 会发生错误
                 * 采样率:44.1k;采样率位数:16位;立体声(stereo);是否签名;true:
                 * big-endian字节顺序,false:little-endian字节顺序(详见:ByteOrder类)
                 */
                AudioFormat audioFormat = new AudioFormat(44100.0F, 16, 2, true, false);

                // 通过AudioSystem获取本地音频混合器信息
                Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();
                // 通过AudioSystem获取本地音频混合器
                Mixer mixer = AudioSystem.getMixer(minfoSet[AUDIO_DEVICE_INDEX]);
                // 通过设置好的音频编解码器获取数据线信息
                DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
                try {
                    // 打开并开始捕获音频
                    // 通过line可以获得更多控制权
                    // 获取设备：TargetDataLine line
                    // =(TargetDataLine)mixer.getLine(dataLineInfo);
                    final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
                    line.open(audioFormat);
                    line.start();
                    // 获得当前音频采样率
                    final int sampleRate = (int) audioFormat.getSampleRate();
                    // 获取当前音频通道数量
                    final int numChannels = audioFormat.getChannels();
                    // 初始化音频缓冲区(size是音频采样率*通道数)
                    int audioBufferSize = sampleRate * numChannels;
                    final byte[] audioBytes = new byte[audioBufferSize];

                    ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
                    exec.scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // 非阻塞方式读取
                                int nBytesRead = line.read(audioBytes, 0, line.available());
                                // 因为我们设置的是16位音频格式,所以需要将byte[]转成short[]
                                int nSamplesRead = nBytesRead / 2;
                                short[] samples = new short[nSamplesRead];
                                /**
                                 * ByteBuffer.wrap(audioBytes)-将byte[]数组包装到缓冲区
                                 * ByteBuffer.order(ByteOrder)-按little-endian修改字节顺序，解码器定义的
                                 * ByteBuffer.asShortBuffer()-创建一个新的short[]缓冲区
                                 * ShortBuffer.get(samples)-将缓冲区里short数据传输到short[]
                                 */
                                ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
                                // 将short[]包装到ShortBuffer
                                ShortBuffer sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead);
                                // 按通道录制shortBuffer
                                recorder.recordSamples(sampleRate, numChannels, sBuff);
                            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, 0, (long) 1000 / FRAME_RATE, TimeUnit.MILLISECONDS);
                } catch (LineUnavailableException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();

        // javaCV提供了优化非常好的硬件加速组件来帮助显示我们抓取的摄像头视频
        CanvasFrame cFrame = new CanvasFrame("Capture Preview", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        Frame capturedFrame = null;
        // 执行抓取（capture）过程
        while ((capturedFrame = grabber.grab()) != null) {
            if (cFrame.isVisible()) {
                //本机预览要发送的帧
                cFrame.showImage(capturedFrame);
            }
            //定义我们的开始时间，当开始时需要先初始化时间戳
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
            // 创建一个 timestamp用来写入帧中
            videoTS = 1000 * (System.currentTimeMillis() - startTime);
            //检查偏移量
            if (videoTS > recorder.getTimestamp()) {
                System.out.println("Lip-flap correction: " + videoTS + " : " + recorder.getTimestamp() + " -> "
                        + (videoTS - recorder.getTimestamp()));
                //告诉录制器写入这个timestamp
                recorder.setTimestamp(videoTS);
            }
            // 发送帧
            try {
                recorder.record(capturedFrame);
            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                System.out.println("录制帧发生异常，什么都不做");
            }
        }

        cFrame.dispose();
        try {
            if (recorder != null) {
                recorder.stop();
            }
        } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
            System.out.println("关闭录制器失败");
            try {
                if (recorder != null) {
                    grabber.stop();
                }
            } catch (org.bytedeco.javacv.FrameGrabber.Exception e1) {
                System.out.println("关闭摄像头失败");
                return;
            }
        }
        try {
            if (recorder != null) {
                grabber.stop();
            }
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
            System.out.println("关闭摄像头失败");
        }
    }
}

