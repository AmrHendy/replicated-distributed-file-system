package client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import baseInterface.MasterServerClientInterface;
import lib.FileContent;
import lib.ReplicaLoc;
import lib.WriteMsg;

public class Client {
	private static final String MASTER_INFO_FILE = "client/masterinfo.txt";
	private String masterHostname;
	private String masterIp;
	private Integer masterPort;

	MasterServerClientInterface master;
	Registry registry;

	public Client() throws RemoteException, NotBoundException {
		readMasterSpecs(MASTER_INFO_FILE);
		master = gethandle();
	}

	public void read(String fileName) throws FileNotFoundException, RemoteException, IOException{
		ReplicaLoc[] loc  = master.read(fileName);
		// TODO: Contact primary location

	}
	public void write(FileContent file) throws RemoteException, IOException{
		WriteMsg msg  = master.write(file);
		// System.out.println(msg.getTimeStamp());
		// TODO: Contact primary location
	}

	private MasterServerClientInterface gethandle() throws RemoteException, NotBoundException{
		System.setProperty("java.rmi.server.hostname", masterIp);
		registry = LocateRegistry.getRegistry(masterIp, masterPort);
		return (MasterServerClientInterface) registry.lookup(masterHostname);
	}

	private void readMasterSpecs(String masterInfoFile) {
        Path filePath = Paths.get(masterInfoFile);
        Scanner scanner = null;
        try {
            scanner = new Scanner(filePath);
	        ConcurrentHashMap<Integer, ReplicaLoc> replicaIdToLoc = new ConcurrentHashMap<>();
	        while (scanner.hasNextLine()) {
	            String line = scanner.nextLine();
	            String[] parts = line.split(" ");
	            if (parts.length != 0) {
	            	masterHostname = parts[0];
	                masterIp = parts[1];
	                masterPort = Integer.parseInt(parts[2]);
	            }
	        }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
