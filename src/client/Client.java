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
		ReplicaLoc[] loc  = master.read(fileName);
		System.out.println(loc[0].getName());
		// conection with replca
		ReplicaServerClientInterface replicaServer = gethandle(loc[0]);
		FileContent fileContent = replicaServer.read(fileName);
		System.out.println("Content = " + fileContent.getData());
	}

	public void write(FileContent file) throws RemoteException, IOException{
		WriteMsg msg  = master.write(file);
		System.out.println(msg.getTimeStamp());
		ReplicaLoc replicaLoc = msg.getLoc();
		ReplicaServerClientInterface replicaServer = gethandle(replicaLoc);
		replicaServer.write(msg.getTransactionId(), )
	}
	
	public MasterServerClientInterface gethandle() throws RemoteException, NotBoundException{
		String masterName = "masterServer";
		String masterAdd = "127.0.0.1";
		int masterPort = 54443;
		System.setProperty("java.rmi.server.hostname", masterAdd);
		Registry reg = LocateRegistry.getRegistry(masterAdd,masterPort);
		return (MasterServerClientInterface) reg.lookup(masterName);
	}

	public ReplicaServerClientInterface gethandle(ReplicaLoc primrayReplica) throws RemoteException, NotBoundException{
		String replicaName = primrayReplica.getName();
		String replicaAdd = primrayReplica.getIp();
		int replicaPort = primrayReplica.getIp();
		System.setProperty("java.rmi.server.hostname", replicaAdd);
		Registry reg = LocateRegistry.getRegistry(replicaAdd, replicaPort);
		return (ReplicaServerClientInterface) reg.lookup(replicaName);
	}

	public static void main(String[] args) throws NotBoundException, FileNotFoundException, IOException {
		Client c = new Client();
//		c.read("test1.txt");
		c.write(new FileContent("test1.txt"));
	}
}
