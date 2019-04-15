package demo;

import org.bytedeco.javacv.FrameGrabber;
import utils.Videos;

/**
 * @author YanYuHang
 * @create 2019-03-23-9:15
 */
public class TestVideo {
    public static void main(String[] args) throws FrameGrabber.Exception {
        Videos.recordWebcamAndMicrophone(0,4,"测试.mp4",500,500,25);
    }
}
