package server;


import com.videoCall.AudioReceiver;
import com.videoCall.AudioSender;
import com.videoCall.VideoReceiver;
import com.videoCall.VideoSender;
import server.videoCallRelay.AudioRelayServer;
import server.videoCallRelay.VideoRelayServer;

import java.net.InetAddress;

public class VideoCallManager {
    VideoRelayServer videoRelay;
    AudioRelayServer audioRelay;

    public VideoCallManager(InetAddress client1Addr, InetAddress client2Addr) {
        this.videoRelay = new VideoRelayServer(6555, client1Addr, client2Addr);
        this.audioRelay = new AudioRelayServer(6556, client1Addr, client2Addr);
    }

    public void startCall() {
        videoRelay.start();
        audioRelay.start();
    }
    public void stopCall() {
        videoRelay.stopThread();
        audioRelay.stopThread();
    }
}
