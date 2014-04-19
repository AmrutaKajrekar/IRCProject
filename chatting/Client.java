/**
 * Name: Amruta Kajrekar (amruta@pdx.edu) 
 * CS 594: Internetworking protocols
 * Project: IRC application
 */
package irc.chatting;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client extends Thread {

	private Socket clientSocket;
	private BufferedReader readFromServer;
	private DataOutputStream sendToServer;
	private Thread senderThread;
	private BufferedReader readFromClient;
	private String message;
	private String nick;

	public Client() {

		try {
			clientSocket = new Socket("localhost", 1400);
			readFromServer = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			sendToServer = new DataOutputStream(clientSocket.getOutputStream());
			System.out.println("Connected to Server..");
		} catch (UnknownHostException e) {
			System.out.println("Specified host is unknown");
		} catch (IOException e) {
			System.out.println("Invalid Input. 3");
		}

		// create and start new start and read
		senderThread = new Thread(new Runnable() {
			public void run() {
				try {
					// Read messages from server and print them
					String message;
					boolean isMessageRec;
					while (true) {
						isMessageRec = false;
						while ((message = readFromServer.readLine()) != null) {
							if (isMessageRec == false) {
								isMessageRec = true;
							}
							System.out.println(message);
						}
					}
				} catch (IOException i) {
					System.out.println("Server is closed. Please restart the server and the client.");
				}
			}
		});
		senderThread.start();
	}

	public void run() {

		while (true) {
			try {
				readFromClient = new BufferedReader(new InputStreamReader(
						System.in));
				message = readFromClient.readLine();
				if (message.length() > 512) {
					System.out
							.println("The IRC message entered is greater than allowed length. Please enter message less than 512 characters.\n");
				} else if (message.equals("QUIT")) {

					sendMessageToServer(message + '\n');
					this.stop();
					clientSocket.close();
					break;
				} else if (message.contains("NICK")) {
					String[] temp = message.split(" ");
					if (temp.length == 2
							&& (temp[1] != null || !temp[1].isEmpty() || temp[1] != "")) {
						this.nick = temp[1];
						sendMessageToServer(message + '\n');
					} else {
						System.out.print("Please specify your nickname\n");
					}

				} else if (message.contains("PASS")) {
					sendMessageToServer(message + " " + this.nick + '\n');
				} else if (message.contains("JOIN")) {
					sendMessageToServer(message + " " + this.nick + '\n');
				} else {
					sendMessageToServer(message + '\n');
				}

			} catch (IOException e) {
				System.out.println("Invalid input. Please try again.");
			}
		}

	}

	/**
	 * Sends messages to server
	 * 
	 * @param string
	 * @throws IOException
	 */
	private void sendMessageToServer(String string) throws IOException {
		try {
			sendToServer.writeBytes(string);
			sendToServer.flush();
		} catch (SocketException e) {
			System.out
					.println("Server is closed. Please close all clients and restart the server..");
		}
	}

	/**
	 * Creates and starts a new client
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Client newClient = new Client();
		newClient.start();
	}

}
