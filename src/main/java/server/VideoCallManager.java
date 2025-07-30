package server;


import com.videoCall.AudioReceiver;
import com.videoCall.AudioSender;
import com.videoCall.VideoReceiver;
import com.videoCall.VideoSender;

public class VideoCallManager {
    public static void startCall(String serverIP) {
        // Start video transmission
        new Thread(new VideoSender(serverIP, 5555)).start();
//        new Thread(new VideoReceiver(5556)).start();

        // Start audio transmission
        new Thread(new AudioSender(serverIP, 5557)).start();
        new Thread(new AudioReceiver(5558)).start();
    }
}
