package master;

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

import baseInterface.MasterServerClientInterface;
import lib.FileContent;
import lib.ReplicaLoc;
import lib.WriteMsg;
import lib.Parser;


public class MasterServer extends UnicastRemoteObject implements MasterServerClientInterface{
	/**
	 *
	*/
	private static final long SERIALVERSIONUID = 1L;
	private static final Integer REP_PER_FILE = 3; // number of file replicas
	private static final Integer HEART_BEAT_RATE = 3000;  // in milliseconds
	private static final String REPLICA_FILE_NAME = "master/replicaServers.txt";
	private static Random rand = new Random();

	private ConcurrentHashMap<Integer, ReplicaLoc> replicaIdToLoc; // maping from replica id to replica location
	private ConcurrentHashMap<String, ReplicaLoc[]> fileToRepLoc; // mapping from file to replica locations
	private ArrayList<ReplicaLoc> replcasLoc ;
	private AtomicInteger transID ,timeStamp;

	public MasterServer() throws FileNotFoundException , RemoteException{
		fileToRepLoc = new ConcurrentHashMap<>();
		replicaIdToLoc = new Parser().parse(REPLICA_FILE_NAME);
	}

	@Override
	public ReplicaLoc[] read(String fileName)
	throws FileNotFoundException, IOException, RemoteException {
		if(!fileToRepLoc.containsKey(fileName))
			throw new FileNotFoundException(String.format("File '%s' is not found !", fileName));

		return fileToRepLoc.get(fileName);
	}

	@Override
	public WriteMsg write(FileContent data) throws RemoteException, IOException {
		String fileName = data.getFileName();
		int tID = transID.incrementAndGet();
		int timestamp = timeStamp.incrementAndGet();
		ReplicaLoc[] replicas = null;

		if(!fileToRepLoc.containsKey(fileName)){
			replicas = getRandomReplicas();
			fileToRepLoc.put(fileName, replicas);
		}
		else {
			replicas = fileToRepLoc.get(fileName);
		}

		ReplicaLoc primaryLoc = replicas[0];

		return new WriteMsg(tID, timestamp, primaryLoc);
	}

	public ReplicaLoc[] getRandomReplicas(){
		ReplicaLoc[] replcas = new ReplicaLoc[REP_PER_FILE];
		int numOfReplicas = replicaIdToLoc.size();
		boolean[] visited = new boolean[numOfReplicas];

		for (int i = 0; i < replcas.length; i++) {
			int randomReplica = rand.nextInt(numOfReplicas);
			while (visited[randomReplica])
				randomReplica = rand.nextInt(numOfReplicas);
			visited[randomReplica] = true;
			replcas[i] = replicaIdToLoc.get(randomReplica);
		}
		return replcas;
	}
}
