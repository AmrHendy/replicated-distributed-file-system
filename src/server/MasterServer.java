package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Scanner;


import baseInterface.FileContent;
import baseInterface.MasterServerClientInterface;
import baseInterface.ReplicaLoc;
import baseInterface.WriteMsg;

public class MasterServer implements MasterServerClientInterface{
	private HashMap<String, ReplicaLoc[]> fileRepLocaMap;
	private static String METADATA_FILE_NAME = "metadata.txt";
	private static String REPLICA_FILE_NAME = "replicaServers.txt";
	private static int REP_PER_FILE = 2;
	
	public MasterServer() throws FileNotFoundException {
		File metaData = new File(METADATA_FILE_NAME);
		File repServers = new File(REPLICA_FILE_NAME);

		HashMap<String, ReplicaLoc> nameReplicaLocMap = new HashMap<>();


        Scanner sc = new Scanner(repServers);

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] splited = line.split("\\s+");
            String name = splited[0];
            String ip = splited[1];
            int port = Integer.parseInt(splited[2]);
            nameReplicaLocMap.put(name, new ReplicaLoc(name,ip,port));

        }
        sc.close();
        
        Scanner sc2 = new Scanner(metaData);

        while (sc2.hasNextLine()) {
            String line = sc2.nextLine();
            String[] splited = line.split("\\s+");
            String file = splited[0];
            ReplicaLoc[] replicaLocation = new ReplicaLoc[REP_PER_FILE];
            for (int i = 0; i < replicaLocation.length; i++) {
            	replicaLocation[i] = nameReplicaLocMap.get(splited[i+1]);
			}
            fileRepLocaMap.put(file, replicaLocation);
        }
        sc2.close();
	}

	@Override
	public ReplicaLoc[] read(String fileName) throws FileNotFoundException, IOException, RemoteException {
		if(!fileRepLocaMap.containsKey(fileName)){
			throw new FileNotFoundException();
		}
		
		return fileRepLocaMap.get(fileName);
	}

	@Override
	public WriteMsg write(FileContent data) throws RemoteException, IOException {
		// TODO Auto-generated method stub
		return null;
	}
	public static void main(String[] args) throws FileNotFoundException {
		String masterName = "masterServer";
		String masterAdd = "127.0.0.1";
		int masterPort = 7000;


	}
}
