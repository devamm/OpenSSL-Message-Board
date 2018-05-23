import java.io.*;
import java.net.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.security.Security;
import com.sun.net.ssl.internal.ssl.Provider;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Hashtable;
import java.util.ArrayList;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;


public class MessageServer{
	public static void main (String[] args){
		//verify input
		if (args.length !=1){
			System.out.println("usage: MessageServer <portNumber>");
			return;
		}
		int port;
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException e){
			System.out.println("Specified port not a number");
			return;
		}
		//setup for java secure socket extension
		//verify keystore file
		Security.addProvider(new Provider());
        System.setProperty("javax.net.ssl.keyStore","identity.jks"); 
        System.setProperty("javax.net.ssl.keyStorePassword","PotatoShip");

        try {
        	SSLServerSocketFactory sslFactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
            SSLServerSocket listener = (SSLServerSocket)sslFactory.createServerSocket(port);

            System.out.println("Server running. Listening for requests");
            //loop to keep listening for requests
            while(true){
            	SSLSocket clientSock = (SSLSocket)listener.accept();
            	//hand off socket to service thead
            	ServiceThread service = new ServiceThread(clientSock);
            	service.start();
            }
        } catch (IOException e){
        	System.out.println("Error encountered creating/closing sockets");
        }
	}

}

class ServiceThread extends Thread {
    SSLSocket socket;
    Hashtable<String, String> users;
    ArrayList<String> groups; 

    public ServiceThread(SSLSocket s){
        socket = s;
        users = new Hashtable<String, String>();
        groups = new ArrayList<String>();
    }   

    public void run(){
        //start service here
        boolean login = false; //whether a user is logged in or not
        boolean invalid = false; //true when user provides incorrect password
        boolean validRequest = true; //true when a valid POST,GET,END request is recieved
        DataInputStream inputStream = null;
        DataOutputStream outputStream = null;
        String append = "";

        String groupNames = "";
        boolean hasMessage = false;
        try {
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            startup();
            System.out.println("Accepted connection");
            String username = "";
            while(true){
                //main login screen to authenticate user
                
                if(!login){
                    //display when user has connected but not logged in
                    if(invalid){
                        outputStream.writeUTF("Invalid Password. Re-enter username or type EXIT to disconnect");
                    } else {
                       outputStream.writeUTF("Please enter your username or type EXIT to disconnect"); 
                    }
                    
                    String rec = inputStream.readUTF();
                    if(rec.equals("EXIT")){
                        //disconnect client from server
                        outputStream.writeUTF("Goodbye");
                        outputStream.close();
                        inputStream.close();
                        socket.close();
                        System.out.println("Disconnected");
                        break;
                    }
                    username = rec;
                    outputStream.writeUTF("Enter Password:");
                    
                    //username appended to password as a salt
                    //resulting string is hashed 
                    //done in one step to minimize race condition 
                    String password = SHAHash(inputStream.readUTF()+username);

                    if(!hasUser(username)){
                        //no username exists, create new user
                        addUser(username,password);
                        login = true;
                        invalid = false;
                    } else {
                        //user exists, authenticate
                        if(passMatch(username,password)){
                            login = true;
                            invalid = false;
                            //login authenticated
                        } else{
                            invalid = true;
                            //password does not match, return to login
                        }
                    }

                } else {
                    //user has been authenticated, show message options
                    //user has logged in

                    //populate existing groupes in to arraylist, get all group names into string
                    groupNames = getGroups(groups);
                    
                    if(!validRequest){
                        append = "\nInput not in correct format\n";
                    } else {
                        if(!hasMessage){
                            append = "";
                        }
                    }

                    String available = "\nAvailable groups:\n"+groupNames+"\n";
                    outputStream.writeUTF(append+available+"Enter GET <group> to get messages from a group. Enter POST <group> to post to a group. Enter END to end session");
                    String rec = inputStream.readUTF();
                    String[] segments = rec.split(" ");
                    String request = segments[0];
                    switch(request){
                        case "GET":
                           //check if request is valid
                            if(segments.length != 2){
                                validRequest = false;
                                hasMessage = false;
                                break;
                            }
                            segments[1] = segments[1].toLowerCase();
                            validRequest = true;
                            hasMessage = true;
                            //get messages from group
                            String msg = getMessages(segments[1]);
                            if(!msg.equals("")){
                                append = "\nMessages from group "+segments[1]+":\n"+msg;
                            } else {
                                append = "\nGroup "+segments[1]+" not found";
                            }

                            break;
                        case "POST":
                            //check if request is valid
                            if(segments.length != 2){
                                validRequest = false;
                                hasMessage = false;
                                break;
                            }
                            segments[1] = segments[1].toLowerCase();
                            validRequest = true;
                            hasMessage = true;

                            //post to group
                            outputStream.writeUTF("Write a message to post in "+segments[1]);
                            String post = inputStream.readUTF();
                            File file = new File("messages/"+segments[1]+".txt");
                            if(!file.exists()){
                                //create new file first
                                file.createNewFile();
                            }
                            //append to file
                            PrintWriter fileOut = new PrintWriter(new BufferedWriter(new FileWriter("messages/"+segments[1]+".txt", true)));
                            String datePosted = getDate(System.currentTimeMillis());
                            //System.out.println(datePosted);
                            post +=("\nposted by "+username+ " on "+datePosted);
                            fileOut.println(post+"\n");
                            fileOut.close();
                            append = "\nPosted to group "+segments[1]+"\n";

                            break;
                        case "END":
                            login = false;
                            hasMessage = false;
                            break;
                        default: 
                            validRequest = false;
                            hasMessage = false;
                            break;
                    } 
                }

            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static String SHAHash (String pass){
        //returns SHA hash of a string
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte[] hash = d.digest(pass.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e){
            System.out.println("Error encountered while hashing password");
        }
        return null;
    }

    private void startup() {
        //loads user password pairs from txt file into hashtable 
        String line = null;
        try {
            FileReader filereader = new FileReader("user.txt");
            BufferedReader reader = new BufferedReader(filereader);
            while ((line = reader.readLine()) != null ){
                String[] split = line.split(",");
                users.put(split[0], split[1]);
            }
            reader.close();

        } catch(Exception e){
            System.out.println("Unable to find file");
        }
    }

    private boolean hasUser(String user){
        //checks if username exits 
        if(users == null){
            return false;
        }
        return users.containsKey(user);
    }

    private boolean passMatch (String user, String pass){
        //sees if a user's entered password matches the password on file
        String passFile = users.get(user);
        return passFile.equals(pass);
    }

    private void addUser (String user, String pass){
        //adds new user into hashtable and file 
        users.put(user,pass);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("user.txt",true));
            writer.append(user+","+pass+"\n");
            writer.close();

        } catch (Exception e){
            System.out.println("error writing to file");
        }
        System.out.println("adding user "+user);
    }

   private static String getMessages(String group){
        //retrieves all messages from a given group
        String line = null;
        String message = "";
        try{
            FileReader fr = new FileReader("messages/"+group+".txt");
            BufferedReader reader = new BufferedReader(fr);

            while((line = reader.readLine()) != null){
                message += line;
                message +="\n";
            }
            
        } catch (Exception ex){
            //seize the means of production
        }
        return message;
    }
    
    private static String getGroups(ArrayList<String> groups){
        //each group stored as infividual text file
        //each group name is inserted into hashtable 
        String names = "";
        File[] files = new File("messages").listFiles();
        String fileName = "";
        for(File file : files){
            fileName = file.getName();
            fileName = fileName.substring(0, fileName.length()-4);
            if(!groups.contains(fileName) && file.isFile()){
                groups.add(fileName);
            }
        }
        //return string of all groups names
        for(int x = 0; x < groups.size(); x++){
            names+=groups.get(x);   
            names+="\n";    
            
        }       
        return names;
    }     

    public static String getDate(long time){
        //converts millisec from epoch into a string with dd MMM yyyy time format 
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        //System.out.println(format.format(date));
        return (format.format(date));
    }

}