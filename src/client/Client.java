package client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import baseInterface.FileContent;
import baseInterface.MasterServerClientInterface;
import baseInterface.ReplicaLoc;
import baseInterface.WriteMsg;

public class Client {
	MasterServerClientInterface master;
	public Client() throws RemoteException, NotBoundException {
		master = gethandle();
	}
	
	public void read(String fileName) throws FileNotFoundException, RemoteException, IOException{
		ReplicaLoc loc  = master.read(fileName)[0];
		System.out.println(loc.getName());
		// conection with replca
		
	}
	public void write(FileContent file) throws RemoteException, IOException{
		WriteMsg msg  = master.write(file);
		System.out.println(msg.getTimeStamp());

		
	}
	
	public MasterServerClientInterface gethandle() throws RemoteException, NotBoundException{
		String masterName = "masterServer";
		String masterAdd = "127.0.0.1";
		int masterPort = 54443;
//		String direction = "‎⁨⁨hosamelsafty⁩";
		System.setProperty("java.rmi.server.hostname", masterAdd);
		Registry reg = LocateRegistry.getRegistry(masterAdd,masterPort);
		System.out.println( reg.lookup(masterName).getClass());;
		return master;
	}
	public static void main(String[] args) throws NotBoundException, FileNotFoundException, IOException {
		Client c = new Client();
		c.read("test1.txt");
	}
}
