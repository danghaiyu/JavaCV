package utils;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.*;
import javax.swing.*;

/**
 * @author YanYuHang
 * @create 2019-03-22-9:55
 */
public class Camera {
    public static void Cameras(String outFile, double framRate) {
        try {
            FrameGrabber grabber = FrameGrabber.createDefault(0);//本机摄像头
            grabber.start();//开始抓取
            //转换器
            OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
            opencv_core.IplImage iplImage = converter.convert(grabber.grab());
            int width = iplImage.width();
            int height = iplImage.height();
            FrameRecorder recorder = FrameRecorder.createDefault(outFile, width, height);
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("flv");
            recorder.setFrameRate(framRate);
            recorder.start();//开始录制
            long startTime = 0;
            long videoTs = 0;
            CanvasFrame frame = new CanvasFrame("摄像头", CanvasFrame.getDefaultGamma() / grabber.getGamma());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setAlwaysOnTop(true);
            Frame rotframe = converter.convert(iplImage);
            while (frame.isVisible() && (iplImage = converter.convert(grabber.grab())) != null) {
                rotframe = converter.convert(iplImage);
                frame.showImage(rotframe);
                if (startTime == 0) {
                    startTime = System.currentTimeMillis();
                }
                videoTs = 1000 * (System.currentTimeMillis() - startTime);
                recorder.setTimestamp(videoTs);
                recorder.record(rotframe);
                Thread.sleep(50);
            }
            frame.dispose();
            recorder.stop();
            recorder.release();
            grabber.stop();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        } catch (FrameRecorder.Exception e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
