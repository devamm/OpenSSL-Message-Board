# CS419 Programming Assignment - OpenSSL Message Board
This Java program simulates an online message-board that uses OpenSSL for fully encrypted communication between the server and client program. The client program is used to connect to the server, which manages the actual message board program.

# Program Security
This project was intended to demonstrate the different foundations of computer security such as encrpytion, authorization, and identification. Users must provide a valid username and login combination in order to access the message board system. Username and password pairs are stored at rest by the server program, and the passowrds are stored as salted hashes. 

Encrpytion is provided through the use of the Java Secure Sockets Extension, which utlizes OpenSSL certificates to not only authenticate the initial handshake between the two, but also to provide end-to-end encrpytion.

# Program Operation
The server runs on localhost, with a specified port number. When the client connects to the server program, a user must enter their credentials in order to access the messaging system. Upon successful login, a user is presented with all available groups. A user can either enter a GET command to view all messages in a certain group, or they can post to a certian group. All messages are stored at rest on the server side. 
