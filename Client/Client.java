import java.io.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.security.Security;
import com.sun.net.ssl.internal.ssl.Provider;

public class Client {
    public static void main(String args[]){
        //verify input
        if (args.length != 1){
            System.out.println("usage: Client <portNumber>");
            return;
        }

        int port;
        try{
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e){
            System.out.println("Specified port not a number");
            return;
        }
      
        //JSSE setup
        //identifies trust.jks as client truststore file
        Security.addProvider(new Provider());
        System.setProperty("javax.net.ssl.trustStore","trust.jks"); 
        System.setProperty("javax.net.ssl.trustStorePassword","XXXX");
        //passwords removed for GitHub upload
        try{
            
            SSLSocketFactory sslsocketfactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket)sslsocketfactory.createSocket("localhost",port);
            
            DataOutputStream out = new DataOutputStream(sslSocket.getOutputStream());
            DataInputStream input = new  DataInputStream(sslSocket.getInputStream());
           
            System.out.println(input.readUTF());
            while (true) {
               String send = System.console().readLine();
               out.writeUTF(send);
               System.out.println(input.readUTF());
               if(send.equals("EXIT")){
                break;
               }
            }
            out.close();
            input.close();
            sslSocket.close();
            
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
