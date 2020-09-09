package pb.protocols.keepalive;


import java.util.logging.Logger;
import pb.Utils;
import pb.Endpoint;
import pb.EndpointUnavailable;
import pb.Manager;
import pb.client.ClientManager;
import pb.protocols.Message;
import pb.protocols.Protocol;
import pb.protocols.IRequestReplyProtocol;
import pb.server.ServerManager;

/**
 * Provides all of the protocol logic for both client and server to undertake
 * the KeepAlive protocol. In the KeepAlive protocol, the client sends a
 * KeepAlive request to the server every 20 seconds using
 * {@link pb.Utils#setTimeout(pb.protocols.ICallback, long)}. The server must
 * send a KeepAlive response to the client upon receiving the request. If the
 * client does not receive the response within 20 seconds (i.e. at the next time
 * it is to send the next KeepAlive request) it will assume the server is dead
 * and signal its manager using
 * {@link pb.Manager#endpointTimedOut(Endpoint,Protocol)}. If the server does
 * not receive a KeepAlive request at least every 20 seconds (again using
 * {@link pb.Utils#setTimeout(pb.protocols.ICallback, long)}), it will assume
 * the client is dead and signal its manager. Upon initialisation, the client
 * should send the KeepAlive request immediately, whereas the server will wait
 * up to 20 seconds before it assumes the client is dead. The protocol stops
 * when a timeout occurs.
 * 
 * @see {@link pb.Manager}
 * @see {@link pb.Endpoint}
 * @see {@link pb.protocols.Message}
 * @see {@link pb.protocols.keepalive.KeepAliveRequest}
 * @see {@link pb.protocols.keepalive.KeepAliveReply}
 * @see {@link pb.protocols.Protocol}
 * @see {@link pb.protocols.IRequestReplyProtocol}
 * @author aaron
 *
 */
public class KeepAliveProtocol extends Protocol implements IRequestReplyProtocol {
	private static Logger log = Logger.getLogger(KeepAliveProtocol.class.getName());
	
	/**
	 * Name of this protocol. 
	 */
	public static final String protocolName="KeepAliveProtocol";

	/**
	 * check whether receive reply after 20 sec
	 */
	private boolean serverTimeoutFlag = true;
	private boolean clientTimeoutFlag = true;

	/**
	 * Initialise the protocol with an endopint and a manager.
	 * @param endpoint
	 * @param manager
	 */
	public KeepAliveProtocol(Endpoint endpoint, Manager manager) {
		super(endpoint,manager);
	}
	
	/**
	 * @return the name of the protocol
	 */
	@Override
	public String getProtocolName() {
		return protocolName;
	}

	/**
	 * Generic stop session call, for either client or server.
	 */
	@Override
	public void stopProtocol() {
		log.severe("protocol stopped while it is still underway");
	}
	
	/*
	 * Interface methods
	 */
	
	/**
	 * 
	 */
	public void startAsServer() {
           checkClientTimeout();
	}
	
	/**
	 *
	 */
	public void checkClientTimeout() {
		    clientTimeoutFlag = true;
			Utils.getInstance().setTimeout(() -> {
				if (clientTimeoutFlag) {
					manager.endpointTimedOut(endpoint, this);
				}else{
					checkClientTimeout();
				}
			}, 10000);

	}
	
	/**
	 * Called by the manager that is acting as a client.
	 */
	public void startAsClient() throws EndpointUnavailable {
		sendRequest(new KeepAliveRequest());
	}

	/**
	 * Just send a request, nothing special.
	 * If manager is Client Manager, then set up a timer task to check timeout
	 * @param msg Request Message to send
	 */
	@Override
	public void sendRequest(Message msg) throws EndpointUnavailable {
		endpoint.send(msg);
		// timeout flag for Client
		serverTimeoutFlag = true; //假设sever会超时
		if (manager instanceof ClientManager) {
			// set up a timer to check whether timeout
			Utils.getInstance().setTimeout(() -> {
				if (serverTimeoutFlag) {
					manager.endpointTimedOut(endpoint, this);
				} else {
					try {
						sendRequest(new KeepAliveRequest());
					} catch (EndpointUnavailable e) {
						// ignore
					}
				}
			}, 10000);
		}
	}

	/**
	 * If the manager is Client Manager then set timeout flag as false.
	 * @param msg Reply Message received
	 */
	@Override
	public void receiveReply(Message msg) {
		if (manager instanceof ClientManager) {
			serverTimeoutFlag = false; //收到reply才没超时
		}
	}

	/**
	 * Received a request then send Reply message back.
	 * If manager is Server Manager, then set timeout flag as false.
	 * @param msg Request Message received
	 * @throws EndpointUnavailable EndpointUnavailable
	 */
	@Override
	public void receiveRequest(Message msg) throws EndpointUnavailable {
		if (manager instanceof ServerManager) {
			clientTimeoutFlag = false;
		}
		if (msg instanceof KeepAliveRequest) {
			sendReply(new KeepAliveReply());
		}


	}

	/**
	 * Send a Reply Message.
	 * If manager is Server Manager, then set timeout flag as false.
	 * And set up a timer task to check if received a request in timeout.
	 * @param msg Reply Message to send.
	 */
	@Override
	public void sendReply(Message msg) throws EndpointUnavailable {
		endpoint.send(msg);
	}
	
	
}
