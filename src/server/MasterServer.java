package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Timer;
import java.util.TimerTask;

import baseInterface.FileContent;
import baseInterface.MasterServerClientInterface;
import baseInterface.MasterServerReplicaServerInterface;
import baseInterface.ReplicaLoc;
import baseInterface.ReplicaServerReplicaServerInterface;
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

		// Create a new thread to run the hear beat with the replica servers
		Timer heartBeatTimer = new Timer();  
		heartBeatTimer.scheduleAtFixedRate(new HeartBeatTask(), 0, heartBeatRate);
	}

	@Override
	public ReplicaLoc[] read(String fileName) throws FileNotFoundException, IOException, RemoteException {
		System.out.println("Receiving Read Request From the Client");
		if(!fileReplicaMap.containsKey(fileName)){
			throw new FileNotFoundException();
		}
		return fileReplicaMap.get(fileName);
	}

	private void assignNewPrimraryReplica(String fileName){
		ReplicaLoc[] replicas = fileReplicaMap.get(fileName);
		ReplicaLoc[] newFileReplicas = new ReplicaLoc[REP_PER_FILE];
		boolean newPrimaryAssigned = false;
		for (ReplicaLoc replicaLoc : replicas) {
			if (replicaLoc.getAlive()){
				newPrimaryAssigned = true;
				newFileReplicas[0] = replicaLoc;
				break;
			}
		}
		int index = 1;
		for (ReplicaLoc replicaLoc : replicas) {
			if(index == REP_PER_FILE){
				// that means all file replicas arenot alive then we canot choose any of them to be primary
				// we can think of choose 3 news replicas, but about the existing files on the old 3 replicas
			}
			if (!replicaLoc.getName().equals(newFileReplicas[0].getFileName()){
				newFileReplicas[index] = replicaLoc;
				index++;
			}
		}
		// assign the new replicas to the file
		fileReplicaMap.put(fileName, newFileReplicas);
	}

	@Override
	public WriteMsg write(FileContent data) throws RemoteException, IOException, NotBoundException {
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
		// check if the primary replica server is not available, then choose another primray replica for that file
		if(!primaryLoc.getAlive()){
			assignNewPrimraryReplica(fileName);
			replicas = fileReplicaMap.get(fileName);
			primaryLoc = replicas[0];
		}

		Registry registry = LocateRegistry.getRegistry(primaryLoc.getIp(), primaryLoc.getPort());
        MasterServerReplicaServerInterface stub = (MasterServerReplicaServerInterface) registry.lookup(primaryLoc.getName());
        
        ArrayList<ReplicaLoc> slaves = new ArrayList<ReplicaLoc>();
        for( int i = 1 ; i < replicas.length ; i++) {
        	slaves.add(replicas[i]) ;
        }
        stub.registerSlaves(fileName, slaves);
        
		return new WriteMsg(tID, timestamp, primaryLoc);
	}

	private ReplicaLoc[] getRandomReplica(){
		Random rand = new Random();
		ReplicaLoc[] replicas = new ReplicaLoc[REP_PER_FILE];
		boolean[] visited = new boolean[replicaLocs.size()];
		for (int i = 0; i < replicas.length; i++) {
			int randomReplica = rand.nextInt(replicaLocs.size());
			while (visited[randomReplica] || !replicaLocs.get(randomReplica).getAlive())
				randomReplica = rand.nextInt(replicaLocs.size());
			visited[randomReplica] = true;
			replicas[i] = replicaLocs.get(randomReplica);
		}
		return replicas;
	}

	//HeartBeat check
	class HeartBeatTask extends TimerTask {
		@Override
		public void run() {
			// check state of replicas
			for (ReplicaLoc replicaLoc : replicaLocs) {
				try {
					nameReplicaLocMap.get(replicaLoc.getName()).checkAlive();
				} catch (RemoteException e) {
					// if an exception occur in rmi then that means the replica is crashed
					// or not available now so we will set its alive to false to handle read or write
					// if that replica is a primary for some files
					replicaLoc.setAlive(false);
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) throws IOException {
		// binding the stub for rmi call
		Controller c = new Controller();
		c.run();
	}
}
