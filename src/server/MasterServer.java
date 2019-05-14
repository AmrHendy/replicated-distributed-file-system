package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import baseInterface.FileContent;
import baseInterface.MasterServerClientInterface;
import baseInterface.ReplicaLoc;
import baseInterface.WriteMsg;

public class MasterServer extends UnicastRemoteObject implements MasterServerClientInterface{
	
	private static final long serialVersionUID = 1L;
	
	// the first replica  in the list represent the primary replica
	private ConcurrentHashMap<String, ReplicaLoc[]> fileReplicaMap;
	private ConcurrentHashMap<String, ReplicaLoc> nameReplicaLocMap;
	private ArrayList<ReplicaLoc> replicaLocs;
	private static String METADATA_FILE_NAME = "metadata.txt";
	private static String REPLICA_FILE_NAME = "replicaServers.txt";
	private static int REP_PER_FILE = 3;

	private AtomicInteger transID ,timeStamp;
	
	public MasterServer() throws FileNotFoundException , RemoteException{
		// initialize the maps
		nameReplicaLocMap = new ConcurrentHashMap<>();
		fileReplicaMap = new ConcurrentHashMap<>();
		replicaLocs = new ArrayList<>(nameReplicaLocMap.values());	

		// filling the maps from the presistant metadata files on disk
		// reading the replica servers metadata
		File repServers = new File(REPLICA_FILE_NAME);
		Scanner sc = new Scanner(repServers);
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] splited = line.split(" ");
            String name = splited[0];
            String ip = splited[1];
            int port = Integer.parseInt(splited[2]);
			ReplicaLoc replicaLoc = new ReplicaLoc(name, ip, port);
            nameReplicaLocMap.put(name, replicaLoc);
			replicaLocs.add(replicaLoc);
		}
        sc.close();

		// reading the files metadata
		File metaData = new File(METADATA_FILE_NAME);
        sc = new Scanner(metaData);
		while (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] splited = line.split(" ");
            String fileName = splited[0];
            ReplicaLoc[] fileReplicaLocations = new ReplicaLoc[REP_PER_FILE];
            for (int i = 1; i <= fileReplicaLocations.length; i++) {
            	fileReplicaLocations[i-1] = nameReplicaLocMap.get(splited[i]);
			}
            fileReplicaMap.put(fileName, fileReplicaLocations);
        }
		sc.close();
		
        transID = new AtomicInteger(0);
        timeStamp = new AtomicInteger(0);        
	}

	@Override
	public ReplicaLoc[] read(String fileName) throws FileNotFoundException, IOException, RemoteException {
		System.out.println("Receiving Read Request From the Client");
		if(!fileReplicaMap.containsKey(fileName)){
			throw new FileNotFoundException();
		}
		return fileReplicaMap.get(fileName);
	}

	@Override
	public WriteMsg write(FileContent data) throws RemoteException, IOException {
		String fileName = data.getFileName();
		int tID = transID.incrementAndGet();
		int timestamp = timeStamp.incrementAndGet();
		ReplicaLoc[] replicas = null;
		if(!fileReplicaMap.containsKey(fileName)){
			replicas = getRandomReplica();
			fileReplicaMap.put(fileName, replicas);
		}
		else {
			replicas = fileReplicaMap.get(fileName);
		}
		ReplicaLoc primaryLoc = replicas[0];
		return new WriteMsg(tID, timestamp, primaryLoc);
	}

	private ReplicaLoc[] getRandomReplica(){
		Random rand = new Random();
		ReplicaLoc[] replicas = new ReplicaLoc[REP_PER_FILE];
		boolean[] visited = new boolean[replicaLocs.size()];
		for (int i = 0; i < replicas.length; i++) {
			int randomReplica = rand.nextInt(replicaLocs.size());
			while (visited[randomReplica])
				randomReplica = rand.nextInt(replicaLocs.size());
			visited[randomReplica] = true;
			replicas[i] = replicaLocs.get(randomReplica);
		}
		return replicas;
	}

	public static void main(String[] args) throws IOException {
		Controller c = new Controller();
		c.run();
	}
}
