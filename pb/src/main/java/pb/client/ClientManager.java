package pb.client;


import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import pb.Endpoint;
import pb.EndpointUnavailable;
import pb.Manager;
import pb.ProtocolAlreadyRunning;
import pb.Utils;
import pb.protocols.IRequestReplyProtocol;
import pb.protocols.Protocol;
import pb.protocols.keepalive.KeepAliveProtocol;
import pb.protocols.session.SessionProtocol;

/**
 * Manages the connection to the server and the client's state.
 * 
 * @see {@link pb.Manager}
 * @see {@link pb.Endpoint}
 * @see {@link pb.protocols.Protocol}
 * @see {@link pb.protocols.IRequestReplyProtocol}
 * @author aaron
 *
 */
public class ClientManager extends Manager {
	private static Logger log = Logger.getLogger(ClientManager.class.getName());
	private SessionProtocol sessionProtocol;
	private KeepAliveProtocol keepAliveProtocol;
	private Socket socket;
	private int retryTimes;
	private String host;
	private int port;
	private Endpoint endpoint;

	public ClientManager(String host,int port) throws UnknownHostException, IOException {

		retryTimes = 0;
		this.host = host;
		this.port = port;
		socket=new Socket(InetAddress.getByName(host),port);
		this.endpoint = new Endpoint(socket,this);
		endpoint.start();
		
		// simulate the client shutting down after 2mins
		// this will be removed when the client actually does something
		// controlled by the user
		Utils.getInstance().setTimeout(()->{
			try {
				sessionProtocol.stopSession();
			} catch (EndpointUnavailable e) {
				//ignore...
			}
		}, 120000);
		
		
		try {
			// just wait for this thread to terminate
			endpoint.join();
		} catch (InterruptedException e) {
			// just make sure the ioThread is going to terminate
			endpoint.close();
		}
		
		Utils.getInstance().cleanUp();
	}
	
	/**
	 * The endpoint is ready to use.
	 * @param endpoint
	 */
	@Override
	public void endpointReady(Endpoint endpoint) {
		log.info("connection with server established");
		sessionProtocol = new SessionProtocol(endpoint,this);
		try {
			// we need to add it to the endpoint before starting it
			endpoint.handleProtocol(sessionProtocol);
			sessionProtocol.startAsClient();
		} catch (EndpointUnavailable e) {
			log.severe("connection with server terminated abruptly because " + e.getMessage());
			endpoint.close();
		} catch (ProtocolAlreadyRunning e) {
			// hmmm, so the server is requesting a session start?
			log.warning("server initiated the session protocol... weird");
		}
		keepAliveProtocol = new KeepAliveProtocol(endpoint,this);
		try {
			// we need to add it to the endpoint before starting it
			endpoint.handleProtocol(keepAliveProtocol);
			keepAliveProtocol.startAsClient();
		} catch (EndpointUnavailable e) {
			log.severe("connection with server terminated abruptly" + e.getMessage());
			endpoint.close();
		} catch (ProtocolAlreadyRunning e) {
			// hmmm, so the server is requesting a session start?
			log.warning("server initiated the session protocol... weird");
		}
		// reset retry times
		retryTimes = 0;
	}
	
	/**
	 * The endpoint close() method has been called and completed.
	 * @param endpoint
	 */
	public void endpointClosed(Endpoint endpoint) {
		log.info("connection with server terminated");
	}
	
	/**
	 * The endpoint has abruptly disconnected. It can no longer
	 * send or receive data.
	 * @param endpoint
	 */
	@Override
	public void endpointDisconnectedAbruptly(Endpoint endpoint) {
		log.severe("connection with server terminated abruptly");
		retryConnect(endpoint);
	}

	/**
	 * An invalid message was received over the endpoint.
	 * @param endpoint
	 */
	@Override
	public void endpointSentInvalidMessage(Endpoint endpoint) {
		log.severe("server sent an invalid message");
		endpoint.close();
	}
	

	/**
	 * The protocol on the endpoint is not responding.
	 * @param endpoint
	 */
	@Override
	public void endpointTimedOut(Endpoint endpoint,Protocol protocol) {
		log.severe("server has timed out because of protocol " + protocol.getProtocolName());
		//retryConnect(endpoint);
	}

	/**
	 * Retry connection when disconnect, if reconnection exceed max retry times, connection close
	 * @param endpoint
	 */
	private void retryConnect(Endpoint endpoint) {
//		if (retryTimes < 10) {
//			log.info("try to connect to server again");
//			// delay 5 seconds
//			System.out.println("计时器开始");
//			Utils.getInstance().setTimeout(() -> {}, 5000);
//			Utils.getInstance().setTimeout(() -> {
//				if (protocolRequested(endpoint, sessionProtocol) && protocolRequested(endpoint, keepAliveProtocol)) {
//					retryTimes = 0;
//				}
//				System.out.println("计时器停止");
//				++retryTimes;
//			}, 5000);
//		} else {
//
//		}
		if (retryTimes < 10) {
			++retryTimes;
			log.info("try to connect to server again");
			System.out.println("this is " + retryTimes + " times to connect");
			endpoint.close();
			try {
				socket = new Socket(InetAddress.getByName(host), port);
			} catch (IOException e) {
				// ignore
			}
			this.endpoint = new Endpoint(socket, this);
			this.endpoint.start();

			try {
				// just wait for this thread to terminate
				this.endpoint.join();
			} catch (InterruptedException e) {
				// just make sure the ioThread is going to terminate
			}
		} else {
			this.endpoint.close();
		}
	}

	/**
	 * The protocol on the endpoint has been violated.
	 * @param endpoint
	 */
	@Override
	public void protocolViolation(Endpoint endpoint,Protocol protocol) {
		log.severe("protocol with server has been violated: "+protocol.getProtocolName());
		endpoint.close();
	}

	/**
	 * The session protocol is indicating that a session has started.
	 * @param endpoint
	 */
	@Override
	public void sessionStarted(Endpoint endpoint) {
		log.info("session has started with server");
		
		// we can now start other protocols with the server
	}

	/**
	 * The session protocol is indicating that the session has stopped. 
	 * @param endpoint
	 */
	@Override
	public void sessionStopped(Endpoint endpoint) {
		log.info("session has stopped with server");
		endpoint.close(); // this will stop all the protocols as well
	}
	

	/**
	 * The endpoint has requested a protocol to start. If the protocol
	 * is allowed then the manager should tell the endpoint to handle it
	 * using {@link pb.Endpoint#handleProtocol(Protocol)}
	 * before returning true.
	 * @param protocol
	 * @return true if the protocol was started, false if not (not allowed to run)
	 */
	@Override
	public boolean protocolRequested(Endpoint endpoint, Protocol protocol) {
		// the only protocols in this system are this kind...
		try {
			((IRequestReplyProtocol)protocol).startAsClient();
			endpoint.handleProtocol(protocol);
			return true;
		} catch (EndpointUnavailable e) {
			// very weird... should log this
			return false;
		} catch (ProtocolAlreadyRunning e) {
			// even more weird... should log this too
			return false;
		}
	}

}
