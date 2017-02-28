import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.TimerTask;

public class Heartbeat implements Runnable{

    private HashMap<String, Object> heartbeat = new HashMap<String,Object>();
    private HashMap<String, Object> heartbeatOuter = new HashMap<String,Object>();
    private String username = "";

    public Heartbeat(String userName){
        username = userName;
    }
    public void run() {
        try {
            while(true){

                Thread.sleep(30000);
                HashMap<String, byte[]> heartbeat = CryptoFunctions.encrypt(new Long(System.currentTimeMillis()), Start.checksumKey, Start.messageKey);


                heartbeatOuter.put("messageType", "heartbeat");
                heartbeatOuter.put("message",heartbeat);
                heartbeatOuter.put("userName", username);
                Client cl = new Client();

                cl.sendHeartBeats(heartbeatOuter);}
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
