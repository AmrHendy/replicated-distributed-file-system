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
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ConcurrentHashMap<String, ReplicaLoc[]> fileRepLocaMap;
	private ConcurrentHashMap<String, ReplicaLoc> nameReplicaLocMap;
	private static Random rand = new Random();
	private ArrayList<ReplicaLoc> replcasLoc ;
	private static String METADATA_FILE_NAME = "metadata.txt";
	private static String REPLICA_FILE_NAME = "replicaServers.txt";
	private static int REP_PER_FILE = 2;
	private AtomicInteger transID ,timeStamp;
	public MasterServer() throws FileNotFoundException , RemoteException{
		File metaData = new File(METADATA_FILE_NAME);
		File repServers = new File(REPLICA_FILE_NAME);
		nameReplicaLocMap = new ConcurrentHashMap<>();
		fileRepLocaMap = new ConcurrentHashMap<>();
        Scanner sc = new Scanner(repServers);
        Scanner sc2 = new Scanner(metaData);
        transID = new AtomicInteger(0);
        timeStamp = new AtomicInteger(0);
//        File dir = new File(direction);
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] splited = line.split("\\s+");
            String name = splited[0];
            String ip = splited[1];
            int port = Integer.parseInt(splited[2]);
            nameReplicaLocMap.put(name, new ReplicaLoc(name,ip,port));
        }
        sc.close();
        
    	replcasLoc = new ArrayList<>( nameReplicaLocMap.values());	
//        if (dir.mkdirs()) {
//            System.out.format("Directory %s has been created.", dir.getAbsolutePath());
//
//        } else if (dir.isDirectory()) {
//            System.out.format("Directory %s has already been created.", dir.getAbsolutePath());
//
//        } else {
//            System.out.format("Directory %s could not be created.", dir.getAbsolutePath());
//        }
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
		System.out.println("enter");
		if(!fileRepLocaMap.containsKey(fileName)){
			throw new FileNotFoundException();
		}
		
		return fileRepLocaMap.get(fileName);
	}

	@Override
	public WriteMsg write(FileContent data) throws RemoteException, IOException {
		String fileName = data.getFileName();
		int tID = transID.incrementAndGet();
		int timestamp = timeStamp.incrementAndGet();
		ReplicaLoc[] replicas = null;
		if(!fileRepLocaMap.containsKey(fileName)){
			replicas = getRandomReplica();
			fileRepLocaMap.put(fileName, replicas);
		}
		else {
			replicas = fileRepLocaMap.get(fileName);
		}
		ReplicaLoc primaryLoc = replicas[0];
		
		
		return new WriteMsg(tID, timestamp, primaryLoc);
	}
	public ReplicaLoc[] getRandomReplica(){
		ReplicaLoc[] replcas = new ReplicaLoc[REP_PER_FILE];
		boolean[] visited = new boolean[replcasLoc.size()];
		for (int i = 0; i < replcas.length; i++) {
			int randomReplica = rand.nextInt(replcasLoc.size());
			while (visited[randomReplica])
				randomReplica = rand.nextInt(replcasLoc.size());
			visited[randomReplica] = true;
			replcas[i] = replcasLoc.get(i);
		}
		return replcas;
	}
	public static void main(String[] args) throws IOException {
//		MasterServer s = new MasterServer();
//		s.write(new FileContent("test1.txt"));
		Controller c = new Controller();
		c.run();

	}
}
