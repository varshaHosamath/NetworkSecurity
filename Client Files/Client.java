import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class Client {
    private ObjectOutputStream out;
    private Socket clientSocket;
    private HashMap<String,Object> DTS;
    private HashMap<String, Object> outmap = new HashMap<String,Object>();
    private HashMap<String, Object> chatOuter = new HashMap<String,Object>();
    private HashMap<String, Object> chat = new HashMap<String,Object>();
    private HashMap<String, Object> outchat = new HashMap<String,Object>();
    private HashMap<String, Object> HB = new HashMap<String,Object>();

    private HashMap<String, Object> connectionDetails = new HashMap<String,Object>();

    private static Key checksumKeyForChat;
    private static Key messageKeyForChat;

    String hostToConnect = "";
    String userToConnect = "";
    String nonce = "";

    HashMap<String, Object> ticket = new HashMap<String,Object>();

    public void connectToServer(String hostName, int port) throws ClassNotFoundException{
        try {
            clientSocket = new Socket(hostName, port);
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.writeObject(DTS);
            out.flush();
            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());

            outmap = (HashMap<String, Object>)ois.readObject();

            ois.close();

            out.close();

            clientSocket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());

        }
    }



    public HashMap<String, Object> requestlogin(HashMap<String, Object> loginMsg) throws ClassNotFoundException {
        // TODO Auto-generated method stub
        DTS = loginMsg;
        connectToServer("dc02.utdallas.edu", 4444);
        return outmap;
    }



    public void requestToChat(String user) throws ClassNotFoundException {

        messageKeyForChat = (Key)outchat.get("messageKey");
        checksumKeyForChat = (Key)outchat.get("checksumKey");
        //connectionDetails = (HashMap<String, Object>) outchat.get("");
        hostToConnect = (String) outchat.get("userHostname");
        userToConnect = (String) outchat.get("user");
        nonce = (String) outchat.get("nonce");
        ticket = (HashMap<String, Object>) outchat.get("ticket");



        connectionTochat(hostToConnect,4444);



    }



    private void connectionTochat(String hostName, int port) throws ClassNotFoundException {
        // TODO Auto-generated method stub
        try {
            clientSocket = new Socket(hostName, port);
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            HashMap<String, Object> messageToBob = new HashMap<String, Object>();
            messageToBob.put("messageType", "ticket");
            messageToBob.put("message", ticket);
            out.writeObject(messageToBob);
            out.flush();
            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
            Object received = ois.readObject();
            System.out.println(received);
            if(received!=null){
                HashMap<String, Object> recMap = (HashMap<String, Object>)received;
                String mes = (String)CryptoFunctions.decrypt((HashMap<String, byte[]>)recMap.get("message"), checksumKeyForChat, messageKeyForChat);
                System.out.println(mes);
                Scanner sc = new Scanner(System.in);
                String toBob = "";
                while(!mes.equals("quit") && !toBob.equals("quit")){
                    System.out.println("Type something to " + (String)outchat.get("user"));
                    toBob = sc.nextLine();
                    messageToBob = new HashMap<String, Object>();
                    messageToBob.put("messageType", "chat");
                    HashMap<String, byte[]> temp = CryptoFunctions.encrypt(toBob,checksumKeyForChat, messageKeyForChat);
                    messageToBob.put("message", temp);
                    out.writeObject(messageToBob);
                    if(toBob.equals("quit")){
                        System.out.println("Ok, exiting application");
                        System.exit(0);
                    }
                    Object received1 = ois.readObject();
                    if(received1 != null) {
                        recMap = (HashMap<String, Object>)received1;
                        System.out.println("Encrypted message from " + (String)outchat.get("user") + ": " + (HashMap<String, byte[]>)recMap.get("message"));
                        mes = (String)CryptoFunctions.decrypt((HashMap<String, byte[]>)recMap.get("message"), checksumKeyForChat, messageKeyForChat);
                        System.out.println("Decrypted message: " + mes);
                    }
                    else{
                        System.out.println("Connection terminated");
                    }


                }
                System.out.println("Quit message received, exiting application");
                System.exit(0);
            }
            else{
                System.out.println("Connection failed");
            }

            //System.out.println(outmap);
            ois.close();

            out.close();

            clientSocket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());

        }


    }



    public void requestForChat(String userName, String user) throws ClassNotFoundException {
        Random rand = new Random();
        int nounce = rand.nextInt(20000) + 1;
        String nonce = String.valueOf(nounce);
        chat.put("username", userName);
        chat.put("targetUser", user);
        chat.put("nonce", nonce);
        chatOuter.put("messageType", "newSession");
        chatOuter.put("message", chat);

        connectToServerForSession("dc02.utdallas.edu", 4444);
        requestToChat(hostToConnect);



    }



    private void connectToServerForSession(String hostName, int port) throws ClassNotFoundException {
        try {
            clientSocket = new Socket(hostName, port);
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.writeObject(chatOuter);
            out.flush();
            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
            Object ob = ois.readObject();
            if(ob==null){
                System.out.println("Requested user not online");
                Start.selectUserToChat();
            }
            outchat = (HashMap<String, Object>)CryptoFunctions.decrypt((HashMap<String, byte[]>)ob, Start.checksumKey, Start.messageKey);
            ois.close();

            out.close();

            clientSocket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());

        }

    }



    public void sendHeartBeats(HashMap<String, Object> heartbeatOuter) throws UnknownHostException, IOException {
        // TODO Auto-generated method stub
        //HB = heartbeatOuter;
        clientSocket = new Socket("dc02.utdallas.edu", 4444);
        out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.writeObject(heartbeatOuter);
    }

}
