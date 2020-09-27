package pb;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import pb.managers.ClientManager;
import pb.managers.ServerManager;
import pb.managers.endpoint.Endpoint;
import pb.utils.Utils;

/**
 * TODO: for project 2B. Admin Client main. Parse command line options and
 * provide default values. Modify this client to take command line options
 * -shutdown, -force and -vader (explained further below). The client should
 * emit the appropriate event, either: {@link pb.managers.ServerManager#shutdownServer},
 * {@link pb.managers.ServerManager#forceShutdownServer} or
 * {@link pb.managers.ServerManager#vaderShutdownServer} and then simply stop the
 * session and terminate. Make sure the client does not emit the event until the
 * sessionStarted event has been emitted, etc. And the client should attempt to
 * cleanly terminate, not just system exit.
 *
 * @see {@link pb.managers.ClientManager}
 * @see {@link pb.utils.Utils}
 * @author aaron
 *
 */
public class AdminClient  {
	private static Logger log = Logger.getLogger(AdminClient.class.getName());
	private static int port=Utils.serverPort; // default port number for the server
	private static String host=Utils.serverHost; // default host for the server
	private static String password;
	private static volatile boolean forceShutdown=false;
	private static volatile boolean vaderShutdown=false;
	private static volatile boolean Shutdown=false;

	private static void help(Options options){
		String header = "PB Admin Client for Unimelb COMP90015\n\n";
		String footer = "\ncontact aharwood@unimelb.edu.au for issues.";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("pb.Client", header, options, footer, true);
		System.exit(-1);
	}

	public static void main( String[] args ) throws IOException, InterruptedException
	{
		// set a nice log format
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"[%1$tl:%1$tM:%1$tS:%1$tL] %2$s %4$s: %5$s%n");

		// parse command line options
		Options options = new Options();
		options.addOption("port",true,"server port, an integer");
		options.addOption("host",true,"hostname, a string");



		/*
		 * TODO for project 2B. Include a command line option to read a secret
		 * (password) from the user. It can simply be a plain text password entered as a
		 * command line option. Use "password" as the name of the option, i.e.
		 * "-password". Add a boolean option (i.e. it does not have an argument) for
		 * each of the shutdown possibilities: shutdown, force, vader. In otherwords,
		 * the user would enter -shutdown for just regular shutdown, -shutdown -force
		 * for force shutdown and -shutdown -vader for vader shutdown.
		 */

		options.addOption("password",true,"password, a string");
		options.addOption("shutdown",false,"shutdown");
		options.addOption("force",false,"force shutdown");
		options.addOption("vader",false,"vader shutdown");


		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args);
		} catch (ParseException e1) {
			help(options);
		}

		if(cmd.hasOption("password")){
			password = cmd.getOptionValue("password");
		}

		if(cmd.hasOption("shutdown")){
			if(cmd.hasOption("force")){
				forceShutdown = true;
			}
			else if(cmd.hasOption("vader")){
				vaderShutdown = true;
			}else {
				Shutdown = true;
			}
		}

		if(cmd.hasOption("port")){
			try{
				port = Integer.parseInt(cmd.getOptionValue("port"));
			} catch (NumberFormatException e){
				System.out.println("-port requires a port number, parsed: "+cmd.getOptionValue("port"));
				help(options);
			}
		}

		if(cmd.hasOption("host")) {
			host = cmd.getOptionValue("host");
		}

		// start up the client
		log.info("PB Client starting up");

		// the client manager will make a connection with the server
		// and the connection will use a thread that prevents the JVM
		// from terminating immediately
		port = Utils.indexServerPort;
		ClientManager clientManager = new ClientManager(host,port);
		clientManager.start();


		/*
		 * TODO for project 2B. Emit an appropriate shutdown event to the server,
		 * sending the password. Then shutdown the clientManager. The following line
		 * will wait for the client manager session to stop cleanly (or otherwise).
		 * Don't forget that you need to modify ServerMain.java to listen for these
		 * events coming from any client that connects to it.
		 */

		clientManager.on(clientManager.sessionStarted,(eventArgs)-> {
			Endpoint endpoint = (Endpoint) eventArgs[0];
			log.info("Client session started: " + endpoint.getOtherEndpointId());
			if (forceShutdown) {
				endpoint.emit(ServerManager.forceShutdownServer, password);
			} else if (vaderShutdown) {
				endpoint.emit(ServerManager.vaderShutdownServer, password);
			} else if (Shutdown) {
				endpoint.emit(ServerManager.shutdownServer, password);
			}
		});

		clientManager.join();

		Utils.getInstance().cleanUp();

	}
}


//java -cp target/pb2b-0.0.1-SNAPSHOT-jar-with-dependencies.jar pb.AdminClient -password pwd -shutdown