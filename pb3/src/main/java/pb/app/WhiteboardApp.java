package pb.app;

import pb.WhiteboardServer;
import pb.managers.ClientManager;
import pb.managers.PeerManager;
import pb.managers.endpoint.Endpoint;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


/**
 * Initial code obtained from:
 * https://www.ssaurel.com/blog/learn-how-to-make-a-swing-painting-and-drawing-application/
 */
public class WhiteboardApp {
	private static Logger log = Logger.getLogger(WhiteboardApp.class.getName());
	
	/**
	 * Emitted to another peer to subscribe to updates for the given board. Argument
	 * must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String listenBoard = "BOARD_LISTEN";

	/**
	 * Emitted to another peer to unsubscribe to updates for the given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unlistenBoard = "BOARD_UNLISTEN";

	/**
	 * Emitted to another peer to get the entire board data for a given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String getBoardData = "GET_BOARD_DATA";

	/**
	 * Emitted to another peer to give the entire board data for a given board.
	 * Argument must have format "host:port:boardid%version%PATHS".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardData = "BOARD_DATA";

	/**
	 * Emitted to another peer to add a path to a board managed by that peer.
	 * Argument must have format "host:port:boardid%version%PATH". The numeric value
	 * of version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathUpdate = "BOARD_PATH_UPDATE";

	/**
	 * Emitted to another peer to indicate a new path has been accepted. Argument
	 * must have format "host:port:boardid%version%PATH". The numeric value of
	 * version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathAccepted = "BOARD_PATH_ACCEPTED";

	/**
	 * Emitted to another peer to remove the last path on a board managed by that
	 * peer. Argument must have format "host:port:boardid%version%". The numeric
	 * value of version must be equal to the version of the board without the undo
	 * applied, i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoUpdate = "BOARD_UNDO_UPDATE";

	/**
	 * Emitted to another peer to indicate an undo has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the undo applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoAccepted = "BOARD_UNDO_ACCEPTED";

	/**
	 * Emitted to another peer to clear a board managed by that peer. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearUpdate = "BOARD_CLEAR_UPDATE";

	/**
	 * Emitted to another peer to indicate an clear has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearAccepted = "BOARD_CLEAR_ACCEPTED";

	/**
	 * Emitted to another peer to indicate a board no longer exists and should be
	 * deleted. Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardDeleted = "BOARD_DELETED";

	/**
	 * Emitted to another peer to indicate an error has occurred.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardError = "BOARD_ERROR";
	
	/**
	 * White board map from board name to board object 
	 */
	Map<String,Whiteboard> whiteboards;
	
	/**
	 * The currently selected white board
	 */
	Whiteboard selectedBoard = null;
	
	/**
	 * The peer:port string of the peer. This is synonomous with IP:port, host:port,
	 * etc. where it may appear in comments.
	 */
	String peerport = "standalone"; // a default value for the non-distributed version
	
	/*
	 * GUI objects, you probably don't need to modify these things... you don't
	 * need to modify these things... don't modify these things [LOTR reference?].
	 */
	
	JButton clearBtn, blackBtn, redBtn, createBoardBtn, deleteBoardBtn, undoBtn;
	JCheckBox sharedCheckbox ;
	DrawArea drawArea;
	JComboBox<String> boardComboBox;
	boolean modifyingComboBox=false;
	boolean modifyingCheckBox=false;

	/**
	 * The endpoint communicate with server
	 */
	Endpoint endpointToServer = null;


	/**
	 * The endpoint connect to server
	 */
	Map<Integer, Endpoint> clientEndpointToServer = new HashMap<>();

	/**
	 * The endpoint connect to client
	 */
	Map<Integer, Endpoint> serverEndpointToClient = new HashMap<>();

	/**
	 * The client manager which connects to different peer server
	 */

	Map<Integer, ClientManager> clientManagers = new HashMap<>();

	PeerManager peerManager = null;

	ClientManager clientManagerToServer = null;

	/**
	 * Initialize the white board app.
	 */
	public WhiteboardApp(int peerPort, String whiteboardServerHost,
			int whiteboardServerPort) {
		whiteboards=new HashMap<>();

		peerport = whiteboardServerHost + ":" + peerPort;
		// TODO
		PeerManager peerManager = new PeerManager(peerPort);
		this.peerManager = peerManager;
		peerManager.on(PeerManager.peerStarted, (args) -> {
			Endpoint endpoint = (Endpoint)args[0];
			System.out.println("Connection from peer: " + endpoint.getOtherEndpointId());
			int port = getPort(endpoint.getOtherEndpointId());
			serverEndpointToClient.put(port, endpoint);
			System.out.println(serverEndpointToClient);

			endpoint.on(WhiteboardApp.listenBoard, args1 -> {
				String data = (String)args1[0];
				System.out.println("Receive listenboard event " + data);
			}).on(WhiteboardApp.unlistenBoard, args1 -> {
				String data = (String)args1[0];
				System.out.println("Receive unlistenboard event " + data);
			}).on(WhiteboardApp.getBoardData, args1 -> {
				System.out.println("Receive " + WhiteboardApp.getBoardData);
				String boardName = (String)args1[0];
				Whiteboard whiteboard = whiteboards.get(boardName);
				endpoint.emit(WhiteboardApp.boardData, whiteboard.toString());
			}).on(WhiteboardApp.boardPathUpdate, args1 -> {
				String data = (String)args1[0];
				System.out.println("Receive "+WhiteboardApp.boardPathUpdate);
				String boardName = getBoardName(data);
				String pathData = getBoardPaths(data);
				WhiteboardPath path = new WhiteboardPath(pathData);
				long version = getBoardVersion(data);
				Whiteboard whiteboard = whiteboards.get(boardName);
				whiteboard.addPath(path, version - 1);
				drawSelectedWhiteboard();
				for (Endpoint server : serverEndpointToClient.values()) {
					if (!server.getOtherEndpointId().equals(endpoint.getOtherEndpointId())) {
						server.emit(WhiteboardApp.boardPathAccepted, data);
					}
				}
			}).on(WhiteboardApp.boardUndoUpdate, args1 -> {
				System.out.println("Client Receive " + WhiteboardApp.boardUndoAccepted);
				String nameAndVersion = (String)args1[0];
				String boardName = getBoardName(nameAndVersion);
				long version = getBoardVersion(nameAndVersion);
				Whiteboard whiteboard = whiteboards.get(boardName);
				whiteboard.undo(version - 1);
				drawSelectedWhiteboard();
				for (Endpoint server : serverEndpointToClient.values()) {
					if (!server.getOtherEndpointId().equals(endpoint.getOtherEndpointId())) {
						server.emit(WhiteboardApp.boardUndoAccepted, nameAndVersion);
					}
				}
			}).on(WhiteboardApp.boardClearUpdate, args1 -> {
				System.out.println("Receive " + WhiteboardApp.boardClearUpdate);
				String nameAndVersion = (String)args1[0];
				String[] part = nameAndVersion.split("%");
				String whiteboardName = part[0];
				long version = Long.parseLong(part[1]);
				Whiteboard whiteboard = whiteboards.get(whiteboardName);
				if (!whiteboard.clear(version - 1)) {
					System.out.println("version error");
				} else {
					drawSelectedWhiteboard();
					for (Endpoint server : serverEndpointToClient.values()) {
						if (!server.getOtherEndpointId().equals(endpoint.getOtherEndpointId())) {
							server.emit(WhiteboardApp.boardClearAccepted, whiteboard.getNameAndVersion());
						}
					}
				}
			}).on(WhiteboardApp.boardDeleted, args1 -> {

			}).on(WhiteboardApp.boardError, args1 -> {

			});

		}).on(PeerManager.peerStopped, (args) -> {
			Endpoint endpoint = (Endpoint)args[0];
			System.out.println("Disconnected from peer: " + endpoint.getOtherEndpointId());
			int port = getPort(endpoint.getOtherEndpointId());
			serverEndpointToClient.remove(port, endpoint);
		}).on(PeerManager.peerError, (args) -> {
			Endpoint endpoint = (Endpoint)args[0];
			System.out.println("There was an error communicating with the peer: "
					+endpoint.getOtherEndpointId());
			int port = getPort(endpoint.getOtherEndpointId());
			serverEndpointToClient.remove(port, endpoint);
		});
		peerManager.start();

		try {
			ClientManager clientManager = connectToServer(peerManager, whiteboardServerHost, whiteboardServerPort);
			clientManager.start();
			show(peerport);

			clientManager.join();
		} catch (UnknownHostException e) {
			System.out.println("The Whiteboard server host could not be found: "+ whiteboardServerHost);
		} catch (InterruptedException e) {
			System.out.println("Interrupted while trying to send updates to the Whiteboard server");
		}
	}

	private ClientManager connectToServer(PeerManager peerManager, String whiteboardServerHost,
								  int whiteboardServerPort) throws InterruptedException, UnknownHostException {
		ClientManager clientManager = peerManager.connect(whiteboardServerPort, whiteboardServerHost);
		clientManager.on(PeerManager.peerStarted, (args) -> {
			Endpoint endpoint = (Endpoint)args[0];
			endpoint.on(WhiteboardServer.sharingBoard, args1 -> {
				String data = (String)args1[0];
				System.out.println("Received SHARING_BOARD event: " + data + " from server");
				String[] parts = data.split(":");
					try {
						connectToPeer(peerManager, parts[0], Integer.parseInt(parts[1]));
					} catch (InterruptedException e) {
						System.out.println("The peer server host could not be found: " + parts[0]);
					} catch (UnknownHostException e) {
						System.out.println("Interrupted while trying to send updates to the peer server");
					}
					createRemoteBoard(data);
			}).on(WhiteboardServer.unsharingBoard, args1 -> {
				String data = (String)args1[0];
				Integer serverPort = getPort(data);
				System.out.println("Received UNSHARING_BOARD event: " + data + " from server");
				deleteBoard(data);
				int port = getPort(data);
				if (!checkStillConnect(port)) {
					clientManagers.get(port).shutdown();
					clientManagers.remove(port);
				}
			});
			System.out.println("Connected to Whiteboard server: "+endpoint.getOtherEndpointId());
			endpointToServer = endpoint;
			clientManagerToServer = clientManager;
		}).on(PeerManager.peerStopped, (args) -> {
			Endpoint endpoint = (Endpoint)args[0];
			System.out.println("Disconnected from the Whiteboard server: "+endpoint.getOtherEndpointId());
			endpointToServer = null;
			clientManagerToServer = null;
		}).on(PeerManager.peerError, (args) -> {
			Endpoint endpoint = (Endpoint)args[0];
			System.out.println("There was an error communicating with the Whiteboard server: "
					+endpoint.getOtherEndpointId());
			endpointToServer = null;
			clientManagerToServer = null;
		});
		return clientManager;
	}

	private void connectToPeer(PeerManager peerManager, String peerServerHost,
							   int peerServerPort) throws InterruptedException, UnknownHostException {
		ClientManager clientManager = peerManager.connect(peerServerPort, peerServerHost);
		clientManager.on(PeerManager.peerStarted, (args)->{
			Endpoint endpoint = (Endpoint)args[0];
			endpoint.on(WhiteboardApp.boardData, args1 -> {
				System.out.println("Receive " + WhiteboardApp.boardData);
				String data = (String)args1[0];
				String boardName = getBoardName(data);
				Whiteboard whiteboard = whiteboards.get(boardName);
				String boardData = getBoardData(data);
				whiteboard.whiteboardFromString(boardName, boardData);
				drawSelectedWhiteboard();
			}).on(WhiteboardApp.boardClearAccepted,args1 -> {
				System.out.println("Receive " + WhiteboardApp.boardClearAccepted);
				String nameAndVersion = (String)args1[0];
				String[] part = nameAndVersion.split("%");
				String whiteboardName = part[0];
				long version = Long.parseLong(part[1]);
				Whiteboard whiteboard = whiteboards.get(whiteboardName);
				if(!whiteboard.clear(version-1)){
					System.out.println("version error");
				}else{
					drawSelectedWhiteboard();
				}
			}).on(WhiteboardApp.boardPathAccepted, args1 -> {
				System.out.println("client Receive " + WhiteboardApp.boardPathAccepted);
				String data = (String)args1[0];
				String boardName = getBoardName(data);
				String pathData = getBoardPaths(data);
				WhiteboardPath path = new WhiteboardPath(pathData);
				long version = getBoardVersion(data);
				Whiteboard whiteboard = whiteboards.get(boardName);
				whiteboard.addPath(path, version - 1);
				drawSelectedWhiteboard();
			}).on(WhiteboardApp.boardUndoAccepted, args1 -> {
				System.out.println("Client Receive " + WhiteboardApp.boardUndoAccepted);
				String nameAndVersion = (String)args1[0];
				String boardName = getBoardName(nameAndVersion);
				long version = getBoardVersion(nameAndVersion);
				Whiteboard whiteboard = whiteboards.get(boardName);
				whiteboard.undo(version - 1);
				drawSelectedWhiteboard();
			}).on(WhiteboardApp.boardDeleted, args1 -> {
				System.out.println("Client Receive " + WhiteboardApp.boardDeleted);
				String boardName = (String)args1[0];
				deleteBoard(boardName);
			});

			System.out.println("Connected to Peer server: " + endpoint.getOtherEndpointId());
			clientEndpointToServer.put(peerServerPort, endpoint);
			clientManagers.put(peerServerPort, clientManager);
		}).on(PeerManager.peerStopped, (args)->{
			Endpoint endpoint = (Endpoint)args[0];
			System.out.println("Disconnected from the Peer server: " + endpoint.getOtherEndpointId());
			clientEndpointToServer.remove(peerServerPort);
			clientManagers.remove(peerServerPort);
		}).on(PeerManager.peerError, (args)->{
			Endpoint endpoint = (Endpoint)args[0];
			System.out.println("There was an error communicating with the Peer server: "
					+endpoint.getOtherEndpointId());
			clientEndpointToServer.remove(peerServerPort);
			clientManagers.remove(peerServerPort);
		});
		// make sure that do not connect second time
		if (!clientEndpointToServer.containsKey(peerServerPort)) {
			clientManager.start();
		}
	}
	
	/******
	 * 
	 * Utility methods to extract fields from argument strings.
	 * 
	 ******/
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer:port:boardid
	 */
	public static String getBoardName(String data) {
		String[] parts=data.split("%",2);
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return boardid%version%PATHS
	 */
	public static String getBoardIdAndData(String data) {
		String[] parts=data.split(":");
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version%PATHS
	 */
	public static String getBoardData(String data) {
		String[] parts=data.split("%",2);
		return parts[1];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version
	 */
	public static long getBoardVersion(String data) {
		String[] parts=data.split("%",3);
		return Long.parseLong(parts[1]);
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return PATHS
	 */
	public static String getBoardPaths(String data) {
		String[] parts=data.split("%",3);
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer
	 */
	public static String getIP(String data) {
		String[] parts=data.split(":");
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return port
	 */
	public static int getPort(String data) {
		String[] parts=data.split(":");
		return Integer.parseInt(parts[1]);
	}
	
	/******
	 * 
	 * Methods called from events.
	 * 
	 ******/
	
	// From whiteboard server
	// TODO
	private void onShareBoard(boolean share) {
		if (endpointToServer != null) {
			if (share) {
				endpointToServer.emit(WhiteboardServer.shareBoard, selectedBoard.getName());
			} else {
				endpointToServer.emit(WhiteboardServer.unshareBoard, selectedBoard.getName());
			}
		}
	}
	
	// From whiteboard peer

	/**
	 * check whether peer server with given port should keep connection
	 * @param port peer server port
	 */
	private boolean checkStillConnect(int port) {
		for (String boardName : whiteboards.keySet()) {
			if (getPort(boardName) == port) {
				return true;
			}
		}
		return false;
	}

	private void getRemoteBoardData(String name) {
		String[] parts = name.split(":", 2);
		int port = getPort(name);
		Endpoint endpoint = clientEndpointToServer.get(port);
		endpoint.emit(WhiteboardApp.getBoardData, name);
		System.out.println("Emit event " + WhiteboardApp.getBoardData);
		endpoint.emit(WhiteboardApp.listenBoard, name);
		System.out.println("Emit event " + WhiteboardApp.listenBoard);
	}

	/**
	 * Create a remote board.
	 * @param name peer:port:boardid.
	 */
	private void createRemoteBoard(String name) {
		Whiteboard whiteboard = new Whiteboard(name, true);
		addBoard(whiteboard, false);
	}
	
	/******
	 * 
	 * Methods to manipulate data locally. Distributed systems related code has been
	 * cut from these methods.
	 * 
	 ******/
	
	/**
	 * Wait for the peer manager to finish all threads.
	 */
	public void waitToFinish() {
		// TODO
		System.out.println("wait to finish");

	}
	
	/**
	 * Add a board to the list that the user can select from. If select is
	 * true then also select this board.
	 * @param whiteboard
	 * @param select
	 */
	public void addBoard(Whiteboard whiteboard,boolean select) {
		synchronized(whiteboards) {
			whiteboards.put(whiteboard.getName(), whiteboard);
		}
		updateComboBox(select?whiteboard.getName():null);
	}
	
	/**
	 * Delete a board from the list.
	 * @param boardname must have the form peer:port:boardid
	 */
	public void deleteBoard(String boardname) {
		synchronized(whiteboards) {
			Whiteboard whiteboard = whiteboards.get(boardname);
			if(whiteboard!=null) {
				if(whiteboard.isShared()){
					for (Endpoint endpoint : serverEndpointToClient.values()) {
						endpoint.emit(WhiteboardApp.boardDeleted,boardname);
					}
					endpointToServer.emit(WhiteboardServer.unshareBoard,boardname);
				}
				whiteboards.remove(boardname);
			}
		}
		updateComboBox(null);
	}
	
	/**
	 * Create a new local board with name peer:port:boardid.
	 * The boardid includes the time stamp that the board was created at.
	 */
	public void createBoard() {
		String name = peerport+":board"+Instant.now().toEpochMilli();
		Whiteboard whiteboard = new Whiteboard(name,false);
		addBoard(whiteboard,true);
	}
	
	/**
	 * Add a path to the selected board. The path has already
	 * been drawn on the draw area; so if it can't be accepted then
	 * the board needs to be redrawn without it.
	 * @param currentPath
	 */
	public void pathCreatedLocally(WhiteboardPath currentPath) {
		if (selectedBoard != null) {
			if (!selectedBoard.addPath(currentPath, selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard(); // just redraw the screen without the path
			} else {
				// was accepted locally, so do remote stuff if needed
				String pathData = currentPath.toString();
				String data = selectedBoard.getNameAndVersion() + "%" + pathData;
				if (selectedBoard.isRemote()) {
					// emit update event to server peer to request update
					int port = getPort(selectedBoard.getName());
					Endpoint endpoint = clientEndpointToServer.get(port);
					System.out.println("client emit boardPath update");
					endpoint.emit(WhiteboardApp.boardPathUpdate, data);
				} else {
					if (selectedBoard.isShared()) {
						System.out.println("Server emit board path accept");
						for (Endpoint endpoint : serverEndpointToClient.values()) {
							endpoint.emit(WhiteboardApp.boardPathAccepted, data);
						}
					}
				}
			}
		} else {
			log.severe("path created without a selected board: "+currentPath);
		}
	}

	/**
	 * Clear the selected whiteboard.
	 */
	public void clearedLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.clear(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				// was accepted locally, so do remote stuff if needed
				drawSelectedWhiteboard();
				if(!selectedBoard.isRemote()){
					for (Endpoint endpoint : serverEndpointToClient.values()) {
						endpoint.emit(WhiteboardApp.boardClearAccepted,selectedBoard.getNameAndVersion());
					}
				}else{
					for (Endpoint endpoint : clientEndpointToServer.values()) {
						endpoint.emit(WhiteboardApp.boardClearUpdate,selectedBoard.getNameAndVersion());
					}
				}
			}
		} else {
			log.severe("cleared without a selected board");
		}
	}
	
	/**
	 * Undo the last path of the selected whiteboard.
	 */
	public void undoLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.undo(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				if (selectedBoard.isRemote()) {
					int port = getPort(selectedBoard.getName());
					Endpoint endpoint = clientEndpointToServer.get(port);
					endpoint.emit(WhiteboardApp.boardUndoUpdate, selectedBoard.getNameAndVersion());
				} else {
					if (selectedBoard.isShared()) {
						for (Endpoint endpoint : serverEndpointToClient.values()) {
							endpoint.emit(WhiteboardApp.boardUndoAccepted, selectedBoard.getNameAndVersion());
						}
					}
				}
				drawSelectedWhiteboard();
			}
		} else {
			log.severe("undo without a selected board");
		}
	}
	
	/**
	 * The variable selectedBoard has been set.
	 */
	public void selectedABoard() {
		drawSelectedWhiteboard();
		log.info("selected board: " + selectedBoard.getName());
	}
	
	/**
	 * Set the share status on the selected board.
	 */
	public void setShare(boolean share) {
		if(selectedBoard!=null) {
        	selectedBoard.setShared(share);
			onShareBoard(share);
        } else {
        	log.severe("there is no selected board");
        }
		System.out.println("click to share checkbox in peer : " + share);
	}
	
	/**
	 * Called by the gui when the user closes the app.
	 */
	public void guiShutdown() {
		// do some final cleanup
		HashSet<Whiteboard> existingBoards= new HashSet<>(whiteboards.values());
		existingBoards.forEach((board)->{
			deleteBoard(board.getName());
		});

		clientManagers.values().forEach(ClientManager::shutdown);
		clientManagers.clear();
		peerManager.shutdown();
		clientManagerToServer.shutdown();

	}
	
	

	/******
	 * 
	 * GUI methods and callbacks from GUI for user actions.
	 * You probably do not need to modify anything below here.
	 * 
	 ******/
	
	/**
	 * Redraw the screen with the selected board
	 */
	public void drawSelectedWhiteboard() {
		drawArea.clear();
		if(selectedBoard!=null) {
			selectedBoard.draw(drawArea);
		}
	}
	
	/**
	 * Setup the Swing components and start the Swing thread, given the
	 * peer's specific information, i.e. peer:port string.
	 */
	public void show(String peerport) {
		// create main frame
		JFrame frame = new JFrame("Whiteboard Peer: "+peerport);
		Container content = frame.getContentPane();
		// set layout on content pane
		content.setLayout(new BorderLayout());
		// create draw area
		drawArea = new DrawArea(this);

		// add to content pane
		content.add(drawArea, BorderLayout.CENTER);

		// create controls to apply colors and call clear feature
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

		/**
		 * Action listener is called by the GUI thread.
		 */
		ActionListener actionListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == clearBtn) {
					clearedLocally();
				} else if (e.getSource() == blackBtn) {
					drawArea.setColor(Color.black);
				} else if (e.getSource() == redBtn) {
					drawArea.setColor(Color.red);
				} else if (e.getSource() == boardComboBox) {
					if(modifyingComboBox) return;
					if(boardComboBox.getSelectedIndex()==-1) return;
					String selectedBoardName=(String) boardComboBox.getSelectedItem();
					if(whiteboards.get(selectedBoardName)==null) {
						log.severe("selected a board that does not exist: "+selectedBoardName);
						return;
					}
					selectedBoard = whiteboards.get(selectedBoardName);
					// remote boards can't have their shared status modified
					if(selectedBoard.isRemote()) {
						sharedCheckbox.setEnabled(false);
						sharedCheckbox.setVisible(false);
						getRemoteBoardData(selectedBoard.getName());
					} else {
						modifyingCheckBox=true;
						sharedCheckbox.setSelected(selectedBoard.isShared());
						modifyingCheckBox=false;
						sharedCheckbox.setEnabled(true);
						sharedCheckbox.setVisible(true);
					}
					selectedABoard();
				} else if (e.getSource() == createBoardBtn) {
					createBoard();
				} else if (e.getSource() == undoBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to undo");
						return;
					}
					undoLocally();
				} else if (e.getSource() == deleteBoardBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to delete");
						return;
					}
					deleteBoard(selectedBoard.getName());
				}
			}
		};
		
		clearBtn = new JButton("Clear Board");
		clearBtn.addActionListener(actionListener);
		clearBtn.setToolTipText("Clear the current board - clears remote copies as well");
		clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		blackBtn = new JButton("Black");
		blackBtn.addActionListener(actionListener);
		blackBtn.setToolTipText("Draw with black pen");
		blackBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		redBtn = new JButton("Red");
		redBtn.addActionListener(actionListener);
		redBtn.setToolTipText("Draw with red pen");
		redBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		deleteBoardBtn = new JButton("Delete Board");
		deleteBoardBtn.addActionListener(actionListener);
		deleteBoardBtn.setToolTipText("Delete the current board - only deletes the board locally");
		deleteBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		createBoardBtn = new JButton("New Board");
		createBoardBtn.addActionListener(actionListener);
		createBoardBtn.setToolTipText("Create a new board - creates it locally and not shared by default");
		createBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		undoBtn = new JButton("Undo");
		undoBtn.addActionListener(actionListener);
		undoBtn.setToolTipText("Remove the last path drawn on the board - triggers an undo on remote copies as well");
		undoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		sharedCheckbox = new JCheckBox("Shared");
		sharedCheckbox.addItemListener(new ItemListener() {    
	         public void itemStateChanged(ItemEvent e) { 
	            if(!modifyingCheckBox) setShare(e.getStateChange()==1);
	         }    
	      }); 
		sharedCheckbox.setToolTipText("Toggle whether the board is shared or not - tells the whiteboard server");
		sharedCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
		

		// create a drop list for boards to select from
		JPanel controlsNorth = new JPanel();
		boardComboBox = new JComboBox<String>();
		boardComboBox.addActionListener(actionListener);
		
		
		// add to panel
		controlsNorth.add(boardComboBox);
		controls.add(sharedCheckbox);
		controls.add(createBoardBtn);
		controls.add(deleteBoardBtn);
		controls.add(blackBtn);
		controls.add(redBtn);
		controls.add(undoBtn);
		controls.add(clearBtn);

		// add to content pane
		content.add(controls, BorderLayout.WEST);
		content.add(controlsNorth,BorderLayout.NORTH);

		frame.setSize(600, 600);
		
		// create an initial board
		createBoard();
		
		// closing the application
		frame.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent windowEvent) {
		        if (JOptionPane.showConfirmDialog(frame, 
		            "Are you sure you want to close this window?", "Close Window?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
		        {
		        	guiShutdown();
		            frame.dispose();
		        }
		    }
		});
		
		// show the swing paint result
		frame.setVisible(true);
		
	}
	
	/**
	 * Update the GUI's list of boards. Note that this method needs to update data
	 * that the GUI is using, which should only be done on the GUI's thread, which
	 * is why invoke later is used.
	 * 
	 * @param select, board to select when list is modified or null for default
	 *                selection
	 */
	private void updateComboBox(String select) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				modifyingComboBox=true;
				boardComboBox.removeAllItems();
				int anIndex=-1;
				synchronized(whiteboards) {
					ArrayList<String> boards = new ArrayList<String>(whiteboards.keySet());
					Collections.sort(boards);
					for(int i=0;i<boards.size();i++) {
						String boardname=boards.get(i);
						boardComboBox.addItem(boardname);
						if(select!=null && select.equals(boardname)) {
							anIndex=i;
						} else if(anIndex==-1 && selectedBoard!=null && 
								selectedBoard.getName().equals(boardname)) {
							anIndex=i;
						} 
					}
				}
				modifyingComboBox=false;
				if(anIndex!=-1) {
					boardComboBox.setSelectedIndex(anIndex);
				} else {
					if(whiteboards.size()>0) {
						boardComboBox.setSelectedIndex(0);
					} else {
						drawArea.clear();
						createBoard();
					}
				}
				
			}
		});
	}
	
}
