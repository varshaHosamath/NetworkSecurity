import java.util.HashMap;

public class HeartbeatMonitor implements Runnable {
    public void run(){
        while(true) {
            long currentTimestamp = System.currentTimeMillis();
            for(String username : Server.userDB.keySet()){
                HashMap<String, Object> userRow = Server.userDB.get(username);
                Long lastTimestamp = (Long)userRow.get("lastTimestamp");
                Boolean userOn = (Boolean)userRow.get("userOnline");
                if(currentTimestamp - lastTimestamp.longValue() > 90000 && userOn.booleanValue() == true){
                    System.out.println("User " + username + "has not sent heartbeat for 90 seconds, setting user as offline");
                    userRow.put("userOnline", new Boolean(false));
                }
            }
        }
    }
}
