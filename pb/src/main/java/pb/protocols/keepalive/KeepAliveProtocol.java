package pb.protocols.keepalive;

import java.time.Instant;
import java.util.logging.Logger;

import pb.*;
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

	/*
	 * check whether receive reply after 20 sec
	 */
	private boolean timeoutFlag = false;
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
	 * 
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
		//
	}
	
	/**
	 *
	 */
	public void checkClientTimeout() {
		timeoutFlag = false;
		Utils.getInstance().setTimeout(() -> {
			timeoutFlag = true;
		}, 20000);
	}
	
	/**
	 * Called by the manager that is acting as a client.
	 */
	public void startAsClient() throws EndpointUnavailable {
		sendRequest(new KeepAliveRequest());
	}

	/**
	 *
	 * @param msg
	 */
	@Override
	public void sendRequest(Message msg) throws EndpointUnavailable {
		endpoint.send(msg);
		if (manager instanceof ClientManager) {
			checkClientTimeout();
		}
	}

	/**
	 * 
	 * @param msg
	 */
	@Override
	public void receiveReply(Message msg) {
		if (manager instanceof ClientManager) {
			if (timeoutFlag) {
				manager.endpointTimedOut(endpoint, this);
			} else {
				Utils.getInstance().cleanUp();
			}
		}
	}

	/**
	 *
	 * @param msg
	 * @throws EndpointUnavailable 
	 */
	@Override
	public void receiveRequest(Message msg) throws EndpointUnavailable {
		if (manager instanceof ServerManager) {
			if (timeoutFlag) {
				manager.endpointTimedOut(endpoint, this);
			} else {
				Utils.getInstance().cleanUp();
			}
		}
		if (msg instanceof KeepAliveRequest) {
			sendReply(new KeepAliveReply());
		}
	}

	/**
	 * 
	 * @param msg
	 */
	@Override
	public void sendReply(Message msg) throws EndpointUnavailable {
		endpoint.send(msg);
		if (manager instanceof ServerManager) {
			checkClientTimeout();
		}
	}
	
	
}
