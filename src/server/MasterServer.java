package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import baseInterface.FileContent;
import baseInterface.MasterServerClientInterface;
import baseInterface.MasterServerReplicaServerInterface;
import baseInterface.MessageNotFoundException;
import baseInterface.ReplicaLoc;
import baseInterface.WriteMsg;
import replica.ReplicaServer;

public class MasterServer extends UnicastRemoteObject implements MasterServerClientInterface{
	
	private static final long serialVersionUID = 1L;
	
	// the first replica  in the list represent the primary replica
	private ConcurrentHashMap<String, ReplicaLoc[]> fileReplicaMap;
	private ConcurrentHashMap<String, ReplicaLoc> nameReplicaLocMap;
	private ArrayList<ReplicaLoc> replicaLocs;
	private static String MASTER_METADATA = "masterServer.txt";
	private static String METADATA_FILE_NAME = "files_metadata.txt";
	private static String REPLICA_FILE_NAME = "replicaServers.txt";
	private static int REP_PER_FILE = 3;
	private static int Heart_Beat_Rate = 1000 ;
	private static String log_name = "master server" ; 

	private AtomicInteger transID ,timeStamp;
	
	public MasterServer() throws RemoteException{
		// initialize the maps
		nameReplicaLocMap = new ConcurrentHashMap<>();
		fileReplicaMap = new ConcurrentHashMap<>();
		replicaLocs = new ArrayList<>(nameReplicaLocMap.values());	

		// bind MasterServer in registery to allow client to communicate
		try {
			bindRMI();
		} catch (FileNotFoundException e) {
			Logger.getLogger(log_name).log(Level.SEVERE,"Master meta data is not found in master");
			System.exit(-1);
		}
		
		// start the replica servers running
		try {
			runAllReplicas();
		} catch (FileNotFoundException e) {
			Logger.getLogger(log_name).log(Level.SEVERE,"Master meta data is not found in replica");
			System.exit(-1);
		}

		// reading the files metadata
		File metaData = new File(METADATA_FILE_NAME);
        Scanner sc = null;
		try {
			sc = new Scanner(metaData);
		} catch (FileNotFoundException e) {
			Logger.getLogger(log_name).log(Level.SEVERE,"Master meta data is not found in Master");
			System.exit(-1);
		}
		
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
		heartBeatTimer.scheduleAtFixedRate(new HeartBeatTask(), 0, Heart_Beat_Rate);
	}

	@Override
	public ReplicaLoc[] read(String fileName) throws FileNotFoundException, MessageNotFoundException {
		if(!fileReplicaMap.containsKey(fileName)){
			throw new FileNotFoundException();
		}
		ReplicaLoc[] replicas = fileReplicaMap.get(fileName);
		ReplicaLoc primaryLoc = replicas[0];
		// check if the primary replica server is not available, then choose another primray replica for that file
		if(!primaryLoc.getAlive()){
			assignNewPrimraryReplica(fileName);
			replicas = fileReplicaMap.get(fileName);
			primaryLoc = replicas[0];
		}
		return fileReplicaMap.get(fileName);
	}

	private void assignNewPrimraryReplica(String fileName) throws MessageNotFoundException{
		ReplicaLoc[] replicas = fileReplicaMap.get(fileName);
		ArrayList<ReplicaLoc> newFileReplicas = new ArrayList<ReplicaLoc>() ; 
		boolean newPrimaryAssigned = false;
		for (ReplicaLoc replicaLoc : replicas) {
			System.out.println(replicaLoc.getName());
			if (replicaLoc.getAlive()){
				newPrimaryAssigned = true;
				newFileReplicas.add(replicaLoc);
				break;
			}
		}
		System.out.println(newFileReplicas.size());
		
		if(newFileReplicas.isEmpty()) {
			replicas = getRandomReplica();
			newFileReplicas.add(replicas[0]);
		}
		
		int index = 1;
		for (ReplicaLoc replicaLoc : replicas) {
			if(index == REP_PER_FILE){
				// that means all file replicas arenot alive then we canot choose any of them to be primary
				// we can think of choose 3 news replicas, but about the existing files on the old 3 replicas
			}
			if (!replicaLoc.getName().equals(newFileReplicas.get(0).getName()) && replicaLoc.getAlive()){
				newFileReplicas.add(replicaLoc);
				index++;
			}
		}
		
		ReplicaLoc[] replica_loc = new ReplicaLoc[newFileReplicas.size()];
		for(int i = 0 ; i < newFileReplicas.size() ; i++) {
			replica_loc[i] = newFileReplicas.get(i);
		}
		// assign the new replicas to the file
		fileReplicaMap.put(fileName, replica_loc);
	}

	@Override
	public WriteMsg write(FileContent data) throws RemoteException, NotBoundException, MessageNotFoundException  {
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

	private ReplicaLoc[] getRandomReplica() throws MessageNotFoundException{
		Random rand = new Random();
		boolean[] visited = new boolean[replicaLocs.size()];
		
		ArrayList<ReplicaLoc> active = new ArrayList<ReplicaLoc>();
		
		for(int i = 0 ; i < replicaLocs.size() ; i++) {
			if(replicaLocs.get(i).getAlive()) {
				active.add(replicaLocs.get(i));
			}
		}

		Logger.getLogger(log_name).log(Level.INFO,"replicas : " + active.size() + " active " + (REP_PER_FILE - active.size()) + " dead");
		
		if(active.isEmpty()) {
			throw new MessageNotFoundException(); 
		}
				
		ReplicaLoc[] replica_loc = new ReplicaLoc[active.size()];
		for(int i = 0 ; i < active.size() ; i++) {
			replica_loc[i] = active.get(i);
		}
		
		return replica_loc;
	}

	private void bindRMI() throws FileNotFoundException{
		File masterServerFile = new File(MASTER_METADATA);
		Scanner sc = new Scanner(masterServerFile);
		// ignore the first heading line
		String line = sc.nextLine();
		// read the master server information 
		line = sc.nextLine();
		String[] splited = line.split(" ");
		String masterName = splited[0];
		String masterAdd = splited[1];
		int masterPort = Integer.parseInt(splited[2]);
		System.setProperty("java.rmi.server.hostname", masterAdd);
		try {
			Registry registry = LocateRegistry.createRegistry(masterPort);
			registry.bind(masterName, this);
			Logger.getLogger(log_name).log(Level.INFO,masterName + " is alive at address : " + masterAdd + " port : " + masterPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void runAllReplicas() throws FileNotFoundException{
		// filling the maps from the presistant metadata files on disk
		// reading the replica servers metadata
		File repServers = new File(REPLICA_FILE_NAME);
		Scanner sc = new Scanner(repServers);
		// ignore the first heading line
		String line = sc.nextLine();
		while (sc.hasNextLine()) {
            line = sc.nextLine();
            String[] splited = line.split(" ");
            String name = splited[0];
            String ip = splited[1];
			int port = Integer.parseInt(splited[2]);
			String replicaDirectoryPath = splited[3]; 
			ReplicaLoc replicaLoc = new ReplicaLoc(name, ip, port);
            nameReplicaLocMap.put(name, replicaLoc);
			replicaLocs.add(replicaLoc);
			
			// run the replica server in new thread
			Thread replicaServerThread = new Thread(new Runnable() {
				@Override
				public void run() {
					Logger.getLogger(log_name).log(Level.INFO,"Start Replica " + replicaLoc.getName() + " running on address " + replicaLoc.getIp() + " and on port " + replicaLoc.getPort());
					try {
						ReplicaServer replicaServer = new ReplicaServer(replicaLoc, replicaDirectoryPath);
						long now = System.currentTimeMillis(); 
						while(true) {
							long curr = System.currentTimeMillis(); 
							if( (replicaLoc.getName().equals("Replica2") || replicaLoc.getName().equals("Replica3") || replicaLoc.getName().equals("Replica1"))  && curr - now > 5000) {
								Logger.getLogger(log_name).log(Level.INFO, replicaLoc.getName() + " died");
								replicaServer.unbindRMI();
								return;
							}
						}
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			replicaServerThread.setName(replicaLoc.getName());
			replicaServerThread.start();
			
			
		}
        sc.close();
	}

	//HeartBeat check
	class HeartBeatTask extends TimerTask {
		@Override
		public void run() {
			// check state of replicas
			for (ReplicaLoc replicaLoc : replicaLocs) {
				try {
					Registry registry = LocateRegistry.getRegistry(replicaLoc.getIp(), replicaLoc.getPort());
					MasterServerReplicaServerInterface stub = (MasterServerReplicaServerInterface) registry.lookup(replicaLoc.getName());
					stub.checkAlive();
				} catch (RemoteException | NotBoundException e) {
					// if an exception occur in rmi then that means the replica is crashed
					// or not available now so we will set its alive to false to handle read or write
					// if that replica is a primary for some files
					replicaLoc.setAlive(false);
				}
			}
		}
	}

}
