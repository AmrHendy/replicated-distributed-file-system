package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import baseInterface.MasterServerClientInterface;

public class Controller implements CommunicationStrategy {
	@Override
	public void run() {
		String masterName = "masterServer";
		String masterAdd = "127.0.0.1";
		int masterPort = 54443;
		String direction = "‎⁨⁨hosamelsafty⁩";
		System.setProperty("java.rmi.server.hostname", masterAdd);
		try {
			MasterServer master = new MasterServer();
			Registry registry = LocateRegistry.createRegistry(masterPort);
			registry.bind(masterName, master);
			System.out.println(masterName + " is alive at address : " + masterAdd + " port : " + masterPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
