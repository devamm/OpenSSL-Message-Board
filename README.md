# CS419 Spring 2018 Programming Assignment - OpenSSL Message Board
This Java program simulates an online message-board that uses OpenSSL for fully encrypted communication between the server and client program. The client program is used to connect to the server, which manages the actual message board program.

# Program Security
This project was intended to demonstrate the different foundations of computer security such as encrpytion, authorization, and identification. Users must provide a valid username and login combination in order identify themselves and to be authorized to access the message board system. Username and password pairs are stored at rest by the server program, and the passowrds are stored as salted hashes for best security practices. 

Encrpytion is provided through the use of the Java Secure Sockets Extension, which utlizes OpenSSL certificates to not only authenticate the initial handshake between the two, but also to provide end-to-end encrpytion between client and server.

# Program Operation
The server runs on localhost and with a pre-specified port number. The server program itself can support multiple client programs connecting to it simultaneously. When the client connects to the server program the server creates a new thread to handle all client requests while it continues to listen for new incomming connections. In order to login, a user must enter their credentials in order to access the messaging system. Upon a successful login, a user is presented with all available groups. A user can either enter a GET command followed by a group name to view all messages in a certain group, or enter a POST command followed by a group name, which will then prompt the user to enter a message to send to that group.
