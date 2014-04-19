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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

public class Server extends Thread {

	private ServerSocket serverSocket;
	Vector<Connection> usersList;
	private Socket serverAccept;
	private Connection conn;
	private Vector<Room> listOfRooms = new Vector<Room>();

	public Server() {
		try {
			serverSocket = new ServerSocket(1400);
			System.out.println("Server is started. Please run the client now.");
		} catch (IOException e) {
			System.out.println("Invalid input. Please try again.");
		}

	}

	/**
	 * Start the server
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Server server = new Server();
		server.start();

	}

	public void run() {
		usersList = new Vector<Connection>();
		while (true) {
			try {
				serverAccept = serverSocket.accept();
				conn = new Connection(this, serverAccept);
				conn.start();
			} catch (IOException ie) {
				System.out.println(ie);
			}
		}
	}

	class Connection extends Thread {

		private Socket mySocket;
		private String message;
		private BufferedReader readFromClient;
		private DataOutputStream displayToClient;
		private String[] cmdAndMsg;
		private String nick;
		private String password;
		private String display;
		private String tempStr;
		private boolean isNickValid = false;
		private boolean isPasswordSet = false;
		private boolean isFullUsernameSet = false;
		private boolean isInviteRec = false;
		private boolean Uflag;
		private Server parentServer;
		private Connection tempObj;
		private int count;
		private int numUsers;
		private boolean passFlag = false;
		private String username;
		private String hostname;
		private String servername;
		private String realname;
		private boolean Unameflag;
		private String inviteForRoom;

		Connection(Server Server, Socket connectionSocket) {
			this.mySocket = connectionSocket;
			parentServer = Server;
			try {
				readFromClient = new BufferedReader(new InputStreamReader(
						connectionSocket.getInputStream()));
				displayToClient = new DataOutputStream(
						connectionSocket.getOutputStream());
			} catch (IOException IOE) {
				System.out.println("Invalid input");
			}
		}

		public void run() {
			try {

				display = "Please set your username first by using command: USER <username> <hostname> <servername> <realname> \n";
				displayToClient.writeBytes(display);
				displayToClient.flush();

				while (true) {

					if (this.isFullUsernameSet
							&& (this.isNickValid && this.isPasswordSet)) {
						display = "\nWelcome to the IRC application. Use following commands \n NICK <nickname> \n PASS <password> \n LIST \n CREATEROOM <roomname> \n JOIN <roomname> \n INVITE <nickname> <channel> \n LEAVE <roomname> \n BUDDYLIST <roomname> \n BROADCAST <roomname> <message> \n TEXT <receiver nickname> <message> \n QUIT \n";
						displayToClient.writeBytes(display);
					}

					message = readFromClient.readLine();
					cmdAndMsg = message.split(" ");

					// USER - for registering username of the user
					if (cmdAndMsg[0].equals("USER")) {
						if (cmdAndMsg.length == 5
								&& ((cmdAndMsg[1] != null
										|| !cmdAndMsg[1].isEmpty() || cmdAndMsg[1] != "")
										&& (cmdAndMsg[2] != null
												|| !cmdAndMsg[2].isEmpty() || cmdAndMsg[2] != "")
										&& (cmdAndMsg[3] != null
												|| !cmdAndMsg[3].isEmpty() || cmdAndMsg[3] != "") && (cmdAndMsg[4] != null
										|| !cmdAndMsg[4].isEmpty() || cmdAndMsg[4] != ""))) {
							registerUsername();
						} else {
							display = "Please enter all 4 parameters for setting full username.\n";
							displayToClient.writeBytes(display);
						}

					}
					// NICK - for registering nickname of the user
					else if (cmdAndMsg[0].equals("NICK")) {
						if (this.isFullUsernameSet) {
							if (cmdAndMsg.length == 2
									&& ((cmdAndMsg[1] != null
											|| !cmdAndMsg[1].isEmpty() || cmdAndMsg[1] != ""))) {
								registerNickname();
							} else {
								display = "Please specify your nickname\n";
								displayToClient.writeBytes(display);
							}
						} else {
							display = "Please set full username before setting nickname.\n";
							displayToClient.writeBytes(display);
						}

					}
					// PASS - for setting password of the user
					else if (cmdAndMsg[0].equals("PASS")) {
						if (this.isNickValid) {
							if (cmdAndMsg.length == 3
									&& ((cmdAndMsg[1] != null
											|| !cmdAndMsg[1].isEmpty() || cmdAndMsg[1] != ""))) {
								registerPassword();
							} else {
								display = "Please specify your password\n";
								displayToClient.writeBytes(display);
							}
						} else {
							display = "Please set nickname before setting the password.\n";
							displayToClient.writeBytes(display);
						}
					}
					// display a list of users
					else if (cmdAndMsg[0].equals("LIST")
							&& this.isNickValid == true
							&& this.isPasswordSet == true) {
						listOfRooms();
					}
					// create a new room
					else if (cmdAndMsg[0].equals("CREATEROOM")
							&& this.isNickValid == true
							&& this.isPasswordSet == true) {
						if (cmdAndMsg.length == 2
								&& ((cmdAndMsg[1] != null
										|| !cmdAndMsg[1].isEmpty() || cmdAndMsg[1] != ""))) {
							createNewRoom();
						} else {
							display = "Please set roomname while creating room.\n";
							displayToClient.writeBytes(display);
						}
					}
					// invite a user to the room
					else if (cmdAndMsg[0].equals("INVITE")
							&& this.isNickValid == true
							&& this.isPasswordSet == true) {
						if (cmdAndMsg.length == 3
								&& ((cmdAndMsg[1] != null
										|| !cmdAndMsg[1].isEmpty() || cmdAndMsg[1] != ""))
								&& ((cmdAndMsg[2] != null
										|| !cmdAndMsg[2].isEmpty() || cmdAndMsg[2] != ""))) {
							inviteUser();
						} else {
							display = "Please set nickname and channel name while inviting user.\n";
							displayToClient.writeBytes(display);
						}
					}
					// accept the invitation
					else if (cmdAndMsg[0].equals("YES")
							&& this.isNickValid == true
							&& this.isPasswordSet == true) {
						acceptInvite();
					}
					// decline the invitation
					else if (cmdAndMsg[0].equals("NO")
							&& this.isNickValid == true
							&& this.isPasswordSet == true) {
						declineInvite();
					}
					// join an existing room
					else if (cmdAndMsg[0].equals("JOIN")
							&& this.isNickValid == true
							&& this.isPasswordSet == true) {
						if (cmdAndMsg.length == 3
								&& ((cmdAndMsg[1] != null
										|| !cmdAndMsg[1].isEmpty() || cmdAndMsg[1] != ""))) {
							joinRoom();
						} else {
							display = "Please set roomname to which you wish to join.\n";
							displayToClient.writeBytes(display);
						}
					}
					// leave a room
					else if (cmdAndMsg[0].equals("LEAVE")
							&& this.isNickValid == true
							&& this.isPasswordSet == true) {
						if (cmdAndMsg.length == 2
								&& ((cmdAndMsg[1] != null
										|| !cmdAndMsg[1].isEmpty() || cmdAndMsg[1] != ""))) {
							leaveRoom();
						} else {
							display = "Please set roomname to which you wish to leave.\n";
							displayToClient.writeBytes(display);
						}
					}
					// send user a list of his buddies
					else if (this.cmdAndMsg[0].equals("BUDDYLIST")
							&& this.isNickValid == true
							&& this.isPasswordSet == true) {
						if (cmdAndMsg.length == 2
								&& ((cmdAndMsg[1] != null
										|| !cmdAndMsg[1].isEmpty() || cmdAndMsg[1] != ""))) {
							buddyList();
						} else {
							display = "Please enter roomname for the buddylist!\n";
							displayToClient.writeBytes(display);
						}
					}
					// broadcast a message to all the users
					else if (this.cmdAndMsg[0].equals("BROADCAST")
							&& this.isNickValid == true
							&& this.isPasswordSet == true) {
						if (cmdAndMsg.length >= 3
								&& ((cmdAndMsg[1] != null
										|| !cmdAndMsg[1].isEmpty() || cmdAndMsg[1] != ""))
								&& ((cmdAndMsg[2] != null
										|| !cmdAndMsg[2].isEmpty() || cmdAndMsg[2] != ""))) {
							broadcastMsg();
						} else {
							display = "Please set roomname and message for broadcasting!\n";
							displayToClient.writeBytes(display);
						}
					}
					// send a private message
					else if (this.cmdAndMsg[0].equals("TEXT")
							&& this.isNickValid == true
							&& this.isPasswordSet == true) {
						if (cmdAndMsg.length >= 3
								&& ((cmdAndMsg[1] != null
										|| !cmdAndMsg[1].isEmpty() || cmdAndMsg[1] != ""))
								&& ((cmdAndMsg[2] != null
										|| !cmdAndMsg[2].isEmpty() || cmdAndMsg[2] != ""))) {
							privateMessage();
						} else {
							display = "Please set roomname and message for sending private message!\n";
							displayToClient.writeBytes(display);
						}
					}

					// exits the application
					else if (this.cmdAndMsg[0].equals("QUIT")
							&& this.isNickValid == true
							&& this.isPasswordSet == true) {
						quitApplication();
					} else {
						if (isFullUsernameSet) {
							if (this.nick != null && !this.nick.isEmpty()
									&& this.nick != "") {
								if (this.password != null
										&& !this.password.isEmpty()
										&& this.password != "") {
									display = "Please enter correct command to proceed!\n";
									displayToClient.writeBytes(display);

								} else {
									display = "Please enter correct command to set PASS before you proceed!\n";
									displayToClient.writeBytes(display);
								}
							} else {
								display = "Please enter correct command to set NICK before you proceed!\n";
								displayToClient.writeBytes(display);
							}
						} else {
							display = "Please enter correct command to set USER before you proceed!\n";
							displayToClient.writeBytes(display);
						}
					}
				}
			} catch (SocketException e) {
				System.out.println("User is closed.");
			} catch (IOException Exc) {
				System.out.println("Invalid input.");
			}
		}

		/**
		 * This will remove the user from all the rooms and exit the client.
		 * 
		 * @throws IOException
		 */
		private void quitApplication() throws IOException {
			numUsers = parentServer.usersList.size();
			for (count = 0; count < numUsers; count++) {
				tempObj = (Connection) parentServer.usersList.get(count);
				if (tempObj.nick.equals(this.nick)) {
					DataOutputStream outStream = new DataOutputStream(
							tempObj.mySocket.getOutputStream());
					tempObj.isInviteRec = false;
					tempObj.inviteForRoom = "";
					Vector<String> list = findAllMyRooms();
					for (String roomname : list) {
						String str = "LEAVE " + roomname;
						cmdAndMsg = str.split(" ");
						leaveRoom();
					}
					display = "You have successfully exited the IRC application. Thank you!\n";
					outStream.writeBytes(display);
					parentServer.usersList.remove(count);

					break;
				}
			}
		}

		/**
		 * Find in which all rooms this user is joined.
		 * 
		 * @return list of particular user's rooms
		 */
		private Vector<String> findAllMyRooms() {
			Vector<String> myRooms = new Vector<String>();
			// find out the roomname from listOfRooms
			for (Room room : listOfRooms) {
				for (Connection buddy : room.buddyList) {
					if (buddy.nick.equalsIgnoreCase(this.nick)) {
						myRooms.add(room.roomName);
					}
				}
			}
			return myRooms;
		}

		/**
		 * This method gives the channel operator message that user has declined
		 * your invite.
		 * 
		 * @throws IOException
		 */
		private void declineInvite() throws IOException {
			String operator = "";
			for (Room room : listOfRooms) {
				if (this.inviteForRoom.equalsIgnoreCase(room.roomName)) {
					operator = room.channelOperator;
				}
			}
			if (this.isInviteRec) {
				numUsers = parentServer.usersList.size();
				for (count = 0; count < numUsers; count++) {
					tempObj = (Connection) parentServer.usersList.get(count);
					if (tempObj.nick.equalsIgnoreCase(operator)) {
						DataOutputStream outStream = new DataOutputStream(
								tempObj.mySocket.getOutputStream());
						this.isInviteRec = false;
						display = this.nick
								+ " has declined your request to join "
								+ this.inviteForRoom + "\n";
						outStream.writeBytes(display);
						this.inviteForRoom = "";
						display = "Your decline message is sent to the channel operator.";
						displayToClient.writeBytes(display);
						break;
					}
				}
			} else {
				display = "Sorry, you dont have the invitation or you are already joined in the room. \n";
				displayToClient.writeBytes(display);
			}

		}

		/**
		 * The method adds the user to the room when user accepts the invitation
		 * on YES reply
		 * 
		 * @throws IOException
		 */
		private void acceptInvite() throws IOException {
			if (this.isInviteRec) {
				Connection tempBuddy = null;
				boolean isRoomJoined = false;
				boolean isRoomFound = false;
				String channelOperator = "";
				// find out the roomname from listOfRooms
				for (Room room : listOfRooms) {
					if (room.roomName.equalsIgnoreCase(this.inviteForRoom)) {
						isRoomFound = true;
						channelOperator = room.channelOperator;
						for (Connection buddy : room.buddyList) {
							if (buddy.nick.equalsIgnoreCase(this.nick)) {
								isRoomJoined = true;
								break;
							}
						}
					}
				}
				for (Connection user : usersList) {
					if (user.nick.equalsIgnoreCase(channelOperator)) {
						tempBuddy = user;
					}
				}
				if (!isRoomFound) {
					display = "Sorry, No such room exist! \n";
					displayToClient.writeBytes(display);
				}
				if (!isRoomJoined && isRoomFound) {
					DataOutputStream outStream = new DataOutputStream(
							tempBuddy.mySocket.getOutputStream());
					this.isInviteRec = false;
					display = this.nick + " has accepted your request to join "
							+ this.inviteForRoom + "\n";
					outStream.writeBytes(display);
					String str = "JOIN " + this.inviteForRoom + " " + this.nick;
					cmdAndMsg = str.split(" ");
					display = "Your accept message is sent to the channel operator.\n";
					displayToClient.writeBytes(display);
					joinRoom();
					this.inviteForRoom = "";

				} else {
					display = "Sorry, you dont have the invitation or you are already joined in the room. \n";
					displayToClient.writeBytes(display);
				}
			}
		}

		/**
		 * This method is used to send an INVITE to other users to join a room.
		 * 
		 * @throws IOException
		 */
		private void inviteUser() throws IOException {
			// INVITE <nickname> <channel>
			boolean isBuddyFound = false;
			boolean isRoomFound = false;
			boolean isChannelOperator = false;
			// find out the roomname from listOfRooms
			for (Room room : listOfRooms) {
				if (cmdAndMsg[2].equalsIgnoreCase(room.roomName)) {
					isRoomFound = true;
					if (this.nick.equalsIgnoreCase(room.channelOperator)) {
						isChannelOperator = true;

						for (Connection buddy : room.buddyList) {
							if (buddy.nick.equalsIgnoreCase(cmdAndMsg[1])) {
								isBuddyFound = true;
								break;
							}
						}
					}
				}

			}

			if (isRoomFound && !isChannelOperator) {
				display = "Sorry, Only channel operators can INVITE users. \n";
				displayToClient.writeBytes(display);
			}
			if (!isRoomFound) {
				display = "Sorry, No such room found. \n";
				displayToClient.writeBytes(display);
			}
			boolean userFound = false;
			if (isChannelOperator && isRoomFound) {

				if (!isBuddyFound) {
					numUsers = parentServer.usersList.size();
					for (count = 0; count < numUsers; count++) {
						tempObj = (Connection) parentServer.usersList
								.get(count);
						if (tempObj.nick.equals(cmdAndMsg[1])) {
							DataOutputStream outStream = new DataOutputStream(
									tempObj.mySocket.getOutputStream());
							userFound = true;
							display = "Your invite is sent to the user. \n";
							displayToClient.writeBytes(display);
							tempObj.isInviteRec = true;
							tempObj.inviteForRoom = cmdAndMsg[2];
							display = "You have been invited to the room "
									+ cmdAndMsg[2]
									+ ". Reply YES to join room or NO to decline invitation.\n";
							outStream.writeBytes(display);
						}
					}
					if (!userFound) {
						display = "Sorry, No such user exist! \n";
						displayToClient.writeBytes(display);
					}
				} else {
					display = "Sorry, this user is already in the room! \n";
					displayToClient.writeBytes(display);
				}
			}

		}

		private void registerUsername() throws IOException {

			Unameflag = true;
			String uname = cmdAndMsg[1];
			numUsers = parentServer.usersList.size();
			for (count = 0; count < numUsers; count++) {
				tempObj = (Connection) parentServer.usersList.get(count);
				if (tempObj != null && tempObj.username != null
						&& tempObj.username.equals(uname)) {
					System.out.println("Username already exists!!");
					display = "Username already exists.Please enter another username!\n";
					this.Unameflag = false;
					displayToClient.writeBytes(display);
					break;
				}
			}
			if (this.Unameflag) {
				this.username = cmdAndMsg[1];
				this.hostname = cmdAndMsg[2];
				this.servername = cmdAndMsg[3];
				this.realname = cmdAndMsg[4];
				this.isFullUsernameSet = true;
				display = "You have set your full username as \n username = "
						+ this.username
						+ ", hostname = "
						+ this.hostname
						+ ", servername = "
						+ this.servername
						+ ", realname = "
						+ this.realname
						+ "\nNow please set your nickname by using command: NICK <nickname> \n";
				displayToClient.writeBytes(display);
			}

		}

		/**
		 * This method sets password for the particular user.
		 * 
		 * @throws IOException
		 */
		private void registerPassword() throws IOException {

			this.tempStr = cmdAndMsg[2];
			numUsers = parentServer.usersList.size();
			for (count = 0; count < numUsers; count++) {
				tempObj = (Connection) parentServer.usersList.get(count);
				if (tempObj.nick.equals(this.tempStr)) {
					this.password = cmdAndMsg[1];
					this.isPasswordSet = true;
					this.passFlag = true;
					break;
				}
			}

			if (this.passFlag) {
				display = "Hi," + this.nick + ".Your password is set to "
						+ this.password + ". You are now a registered user.\n";
				displayToClient.writeBytes(display);
				displayToClient.flush();
			}

		}

		/**
		 * This method sends private message to a particular user.
		 * 
		 * @throws IOException
		 */
		private void privateMessage() throws IOException {
			// find out the roomname from listOfRooms
			boolean isUserFound = false;
			boolean selfMessage = false;
			for (Connection user : usersList) {
				if (user.nick.equalsIgnoreCase(cmdAndMsg[1])) {
					isUserFound = true;
					if (this.nick.equalsIgnoreCase(cmdAndMsg[1])) {
						selfMessage = true;
						break;
					} else {
						int i, aSize;
						aSize = cmdAndMsg.length;
						DataOutputStream outStream = new DataOutputStream(
								user.mySocket.getOutputStream());
						display = this.nick + ": ";
						outStream.writeBytes(display);
						for (i = 2; i < aSize; i++) {
							display = cmdAndMsg[i] + " ";
							outStream.writeBytes(display);
						}
						outStream.writeBytes("\n");
						break;
					}
				}
			}

			if (!isUserFound) {
				display = "Sorry, No such user exist! \n";
				displayToClient.writeBytes(display);
			} else {
				if (!selfMessage) {
					display = "Your message is sent to " + cmdAndMsg[1] + "\n";
					displayToClient.writeBytes(display);
				} else {
					display = "Sorry, You cannot send message to yourself.\n";
					displayToClient.writeBytes(display);
				}
			}
		}

		/**
		 * this method broadcasts the message send by the user.
		 * 
		 * @throws IOException
		 */
		private void broadcastMsg() throws IOException {
			Room tempRoom = null;
			// find out the roomname from listOfRooms
			for (Room room : listOfRooms) {
				if (room.roomName.equalsIgnoreCase(cmdAndMsg[1])) {
					tempRoom = room;
					break;
				}
			}
			if (tempRoom != null) {
				boolean amIPresentInRoom = false;
				for (Connection buddy : tempRoom.buddyList) {
					if (buddy.nick.equalsIgnoreCase(this.nick)) {
						amIPresentInRoom = true;
						break;
					}
				}
				if (amIPresentInRoom) {
					boolean isMessageBroadcasted = false;
					for (Connection buddy : tempRoom.buddyList) {
						if (!buddy.nick.equalsIgnoreCase(this.nick)) {
							int i, aSize;
							aSize = cmdAndMsg.length;
							DataOutputStream outStream = new DataOutputStream(
									buddy.mySocket.getOutputStream());
							display = this.nick + ": ";
							outStream.writeBytes(display);
							for (i = 2; i < aSize; i++) {
								display = cmdAndMsg[i] + " ";
								outStream.writeBytes(display);
							}
							outStream.writeBytes("\n");
							isMessageBroadcasted = true;
						}
					}
					if (isMessageBroadcasted) {
						display = "Your message is broadcasted to all users in room "
								+ tempRoom.roomName + ". \n";
						displayToClient.writeBytes(display);
					} else {
						display = "Sorry, No other users exist in room "
								+ tempRoom.roomName + ". \n";
						displayToClient.writeBytes(display);
					}
				} else {
					display = "Sorry, You are not joined to the room! Please JOIN room to broadcast message. \n";
					displayToClient.writeBytes(display);
				}
			} else {
				display = "Sorry, No such room exist! \n";
				displayToClient.writeBytes(display);
			}
		}

		/**
		 * This lists the other users in the particular room
		 * 
		 * @throws IOException
		 */
		private void buddyList() throws IOException {
			Room tempRoom = null;
			// find out the roomname from listOfRooms
			for (Room room : listOfRooms) {
				if (room.roomName.equalsIgnoreCase(cmdAndMsg[1])) {
					tempRoom = room;
					break;
				}
			}
			if (tempRoom != null) {
				boolean amIPresentinRoom = false;
				for (Connection buddy : tempRoom.buddyList) {
					if (buddy.nick.equalsIgnoreCase(this.nick)) {
						amIPresentinRoom = true;
						break;
					}
				}
				if (amIPresentinRoom) {
					boolean amIOnlyOne = true;
					for (Connection buddy : tempRoom.buddyList) {
						if (!buddy.nick.equalsIgnoreCase(this.nick)) {
							displayToClient.writeBytes(buddy.nick + "\n");
							displayToClient.flush();
							amIOnlyOne = false;
						}
					}
					if (amIOnlyOne) {
						boolean amIPresent = false;
						for (Connection buddy : tempRoom.buddyList) {
							if (buddy.nick.equalsIgnoreCase(this.nick)) {
								amIPresent = true;
								break;
							}
						}
						if (amIPresent) {
							display = "You are the only user in this room! \n";
							displayToClient.writeBytes(display);
						} else {
							display = "There are no users present in this room! \n";
							displayToClient.writeBytes(display);
						}
					}
				} else {
					displayToClient
							.writeBytes("Sorry, You are not joined to this room. Please join the room to see the buddylist.\n");
					displayToClient.flush();
				}
			} else {
				display = "Sorry, No such room exist! \n";
				displayToClient.writeBytes(display);
			}
		}

		/**
		 * this method logges off the user from the room
		 * 
		 * @throws IOException
		 */
		private void leaveRoom() throws IOException {
			if (this.isNickValid == false) {
				display = "Please register by entering username!\n";
				displayToClient.writeBytes(display);
			} else {
				Room tempRoom = null;
				boolean isRoomJoined = false;
				// find out the roomname from listOfRooms
				for (Room room : listOfRooms) {
					if (room.roomName.equalsIgnoreCase(cmdAndMsg[1])) {
						tempRoom = room;
						break;
					}
				}
				// fine nick name in buddyList
				if (tempRoom != null) {
					for (Connection buddy : tempRoom.buddyList) {
						if (buddy.nick.equalsIgnoreCase(this.nick)) {
							isRoomJoined = true;
							break;
						}
					}
					if (isRoomJoined) {
						tempRoom.buddyList.remove(this);
						display = "You have left the room " + tempRoom.roomName
								+ "!\n";
						displayToClient.writeBytes(display);
						displayToClient.flush();
					} else {
						display = "You are not joined to this room!\n";
						displayToClient.writeBytes(display);
						displayToClient.flush();
					}
				} else {
					display = "Sorry, No such room exist! \n";
					displayToClient.writeBytes(display);
				}
			}
		}

		/**
		 * This method joins the user to the particular room
		 * 
		 * @throws IOException
		 */
		private void joinRoom() throws IOException {
			if (this.isNickValid == false) {
				display = "Please register by entering username!\n";
				displayToClient.writeBytes(display);

			} else {
				Room tempRoom = null;
				boolean isRoomJoined = false;
				// find out the roomname from listOfRooms
				for (Room room : listOfRooms) {
					if (room.roomName.equalsIgnoreCase(cmdAndMsg[1])) {
						tempRoom = room;
						break;
					}
				}
				// fine nick name in buddyList
				if (tempRoom != null) {
					for (Connection buddy : tempRoom.buddyList) {
						if (buddy.nick.equalsIgnoreCase(cmdAndMsg[2])) {
							isRoomJoined = true;
							break;
						}
					}
					if (!isRoomJoined) {
						tempRoom.buddyList.add(this);
						display = "You have joined the room named "
								+ cmdAndMsg[1]
								+ ". Now you can chat with your buddies!\n";
						displayToClient.writeBytes(display);
						displayToClient.flush();
					} else {
						display = "You already have joined the room!\n";
						displayToClient.writeBytes(display);
						displayToClient.flush();
					}

				} else {
					display = "Sorry, No such room exist! \n";
					displayToClient.writeBytes(display);
				}

			}
		}

		/**
		 * this method creates a new room
		 * 
		 * @throws IOException
		 */
		private void createNewRoom() throws IOException {
			if (this.isNickValid == false) {
				display = "Please register by entering username!\n";
				displayToClient.writeBytes(display);
			} else {
				boolean isRoomExist = false;
				for (Room oldRoom : listOfRooms) {
					if (oldRoom.roomName.equalsIgnoreCase(cmdAndMsg[1])) {
						isRoomExist = true;
						break;
					}
				}
				if (!isRoomExist) {
					Room newRoom = new Room();
					newRoom.roomName = cmdAndMsg[1];
					newRoom.channelOperator = this.nick;
					// newRoom.buddyList.add(this);
					listOfRooms.add(newRoom);

					display = "A new room is created named " + cmdAndMsg[1]
							+ ". And you are the channel operator. \n";
					displayToClient.writeBytes(display);
					displayToClient.flush();
				} else {
					display = "A room named "
							+ cmdAndMsg[1]
							+ " already exist! Please specify other roomname.\n";
					displayToClient.writeBytes(display);
					displayToClient.flush();
				}
			}
		}

		/**
		 * This method lists the available rooms
		 * 
		 * @throws IOException
		 */
		private void listOfRooms() throws IOException {
			if (!listOfRooms.isEmpty() && listOfRooms.size() > 0) {
				for (Room room : listOfRooms) {
					displayToClient.writeBytes("Room name is " + room.roomName
							+ " and channel operator is "
							+ room.channelOperator + "\n");
				}
			} else {
				displayToClient.writeBytes("No rooms created! \n");
			}
		}

		/**
		 * This method registers the user
		 * 
		 * @throws IOException
		 */
		private void registerNickname() throws IOException {
			Uflag = true;
			this.tempStr = cmdAndMsg[1];
			numUsers = parentServer.usersList.size();
			for (count = 0; count < numUsers; count++) {
				tempObj = (Connection) parentServer.usersList.get(count);
				if (tempObj != null && tempObj.nick != null
						&& tempObj.nick.equals(this.tempStr)) {
					System.out.println("Nickname already exists!!");
					display = "Nickname already exists.Please enter another nickname!\n";
					this.Uflag = false;
					displayToClient.writeBytes(display);
					break;
				}
			}
			if (this.Uflag) {
				this.nick = tempStr;
				if (this.isNickValid) {
					display = "Your nickname is updated to " + this.nick + ". ";
					displayToClient.writeBytes(display);
					if (!this.isPasswordSet) {
						display = "Please set your password now by using command PASS <password>\n";
						displayToClient.writeBytes(display);
					}
				} else {
					this.isNickValid = true;
					parentServer.usersList.add(this);
					display = "Your nickname is set to "
							+ this.nick
							+ ". Please set your password now by using command PASS <password> \n";
					displayToClient.writeBytes(display);
				}
			}
		}
	}

	class Room {
		private String roomName = "";
		private Vector<Connection> buddyList = new Vector<Connection>();
		private String channelOperator = "";
	}
}
