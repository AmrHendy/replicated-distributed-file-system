import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
// Exceptions
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.IOException;
import lib.MessageNotFoundException;

import java.util.List;
import java.util.ArrayList;

import baseInterface.MasterServerClientInterface;
import master.MasterServer;

public class Server {

	private static String masterHostname = "masterserver";

	@Parameter
	private static List<String> parameters = new ArrayList<>();

	@Parameter(names = "-ip", description =
	 "the ip address of the server. The default value is 127.0.0.1")
	private static String masterIp = "127.0.0.1";

	@Parameter(names = "-port", description =
	 "the port number at which the server will be listening to messages. The default is 8080.")
	private static Integer masterPort = 8080;

	@Parameter(names = "-dir", description = "directory path")
	private static String dir;

	private static MasterServer launchMaster()
	throws AlreadyBoundException, AccessException, RemoteException{
		MasterServer master = null;
		try {
			master = new MasterServer();
			Registry registry = LocateRegistry.createRegistry(masterPort);
			registry.bind(masterHostname, master);
			System.out.println("Master server is up and running...");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// System.setProperty("java.rmi.server.hostname", masterIp);
		// MasterServerClientInterface exportedObject =
		// 		(MasterServerClientInterface) UnicastRemoteObject.exportObject(master, masterPort);
		return master;
	}

	public static void main(String[] args) throws Exception{
		Server s = new Server();
		JCommander.newBuilder()
		  .addObject(s)
		  .build()
		  .parse(args);

		String directory = s.dir;
		// Set up master server
		MasterServer master = launchMaster();

		// TODO: Set up replica servers
	}
}