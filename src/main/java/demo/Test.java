package demo;

import utils.Camera;

/**
 * @author YanYuHang
 * @create 2019-03-22-10:08
 */
public class Test {
    public static void main(String[] args) {
       /* Camera.Cameras("视频.mp4",25);*/
        Camera.Cameras("rtmp://192.168.102.5/shareyyh",25);
    }
}
