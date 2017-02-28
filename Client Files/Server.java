import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.util.HashMap;
import java.util.Scanner;


public class Server implements Runnable {

    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private ServerSocket serverSocket;
    private HashMap<String, Object> req1 = new HashMap<String,Object>();
    private static Key checksumKeyForChat;
    private static Key messageKeyForChat;
    String fromUser = "";

    @Override
    public void run() {
        // TODO Auto-generated method stub
        try {
            serverSocket = new ServerSocket(4444);
            while (true) {
                clientSocket = serverSocket.accept();
                System.out.println("Connection accepted");
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());


                Scanner sc = new Scanner(System.in);
                String toVarsha = "";

                req1 = (HashMap<String, Object>)in.readObject();
                String messageType = (String)req1.get("messageType");
                if(messageType.equals("ticket")){
                    HashMap<String, Object> decryptedTicket = (HashMap<String, Object>)CryptoFunctions.decrypt((HashMap<String, byte[]>)req1.get("message"), Start.checksumKey, Start.messageKey);

                    checksumKeyForChat = (Key)decryptedTicket.get("sharedChecksumKey");
                    messageKeyForChat = (Key) decryptedTicket.get("sharedMessageKey");
                    fromUser = (String) decryptedTicket.get("fromUser");
                    System.out.println(fromUser +" calls, enter 'talk' to talk to " + fromUser);
                    HashMap<String, Object> reply = new HashMap<String, Object>();
                    reply.put("messageType", "ack");
                    HashMap<String, byte[]> temp1 = CryptoFunctions.encrypt("ack", checksumKeyForChat, messageKeyForChat);
                    reply.put("message", temp1);
                    out.writeObject(reply);

                }
                req1 = (HashMap<String, Object>)in.readObject();
                HashMap<String, byte[]> encMessage = (HashMap<String, byte[]>)req1.get("message");
                String decryptedMessage = (String)CryptoFunctions.decrypt(encMessage, checksumKeyForChat, messageKeyForChat);
                while(!toVarsha.equals("quit") && !decryptedMessage.equals("quit")){
                    System.out.println("Encrypted message from " + fromUser + ": " + encMessage);
                    System.out.println("Decrypted message : " +decryptedMessage);
                    System.out.println("Type Something to " + fromUser + ": ");
                    toVarsha = sc.nextLine();
                    HashMap<String, Object> messageToVarsha = new HashMap<String, Object>();
                    messageToVarsha.put("messageType", "chat");
                    HashMap<String, byte[]> temp = CryptoFunctions.encrypt(toVarsha,checksumKeyForChat, messageKeyForChat);
                    messageToVarsha.put("message", temp);
                    out.writeObject(messageToVarsha);
                    if(toVarsha.equals("quit")){
                        System.out.println("Ok, exiting application");
                        System.exit(0);
                    }
                    req1 = (HashMap<String, Object>)in.readObject();
                    encMessage = (HashMap<String, byte[]>)req1.get("message");
                    decryptedMessage = (String)CryptoFunctions.decrypt(encMessage, checksumKeyForChat, messageKeyForChat);
                }
                System.out.println("Quit message received, exiting application");
                System.exit(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Terminating connection");
            //System.exit(-1);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}


