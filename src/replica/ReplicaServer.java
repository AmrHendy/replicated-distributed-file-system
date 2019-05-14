package replica;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import baseInterface.FileContent;
import baseInterface.MasterServerReplicaServerInterface;
import baseInterface.MessageNotFoundException;
import baseInterface.ReplicaLoc;
import baseInterface.ReplicaServerClientInterface;
import baseInterface.ReplicaServerReplicaServerInterface;
import baseInterface.WriteAck;

public class ReplicaServer extends UnicastRemoteObject implements ReplicaServerClientInterface, ReplicaServerReplicaServerInterface, MasterServerReplicaServerInterface{

    private ReplicaLoc replicaLoc;
    private String directoryPath;
    private ConcurrentMap<String, ReentrantReadWriteLock> locks; // locks objects of the open files
    private ConcurrentMap<String, ArrayList<ReplicaServerReplicaServerInterface>> fileSlaveReplicasMap;
    private ConcurrentMap<String, ArrayList<ReplicaLoc>> fileSlaveReplicasLocMap;
    
    // write data
    private Map<Long, String> activeTransactions; // map between active transactions and file names
	private Map<Long, Map<Long, byte[]>> transactionFileMap; // map between transaction ID and corresponding file chunks

    public ReplicaServer(ReplicaLoc replicaLoc, String directoryPath)throws RemoteException{
        this.replicaLoc = replicaLoc;
        this.directoryPath = directoryPath;
        this.locks = new ConcurrentHashMap<String, ReentrantReadWriteLock>();
        this.fileSlaveReplicasLocMap = new ConcurrentHashMap<String, ArrayList<ReplicaLoc>>();
        this.fileSlaveReplicasMap = new ConcurrentHashMap<String, ArrayList<ReplicaServerReplicaServerInterface>>();	
        this.activeTransactions = new HashMap<Long, String>();
        this.transactionFileMap = new HashMap<Long, Map<Long,byte[]>>();
        this.run();
    }

	public WriteAck write(long txnID, long msgSeqNum, FileContent data) throws RemoteException, IOException{
        // if this is not the first message of the write transaction
        if (!transactionFileMap.containsKey(txnID)){
            transactionFileMap.put(txnID, new TreeMap<Long, byte[]>());
            activeTransactions.put(txnID, data.getFileName());
        }
        // TODO save the content in memory or can be saved on temporary local files until comitting
        Map<Long, byte[]> chunkMap =  transactionFileMap.get(txnID);	
        chunkMap.put(msgSeqNum, data.getData().getBytes());
        return new WriteAck(txnID, msgSeqNum);
    }

    @Override
	public FileContent read(String fileName) throws FileNotFoundException, RemoteException, IOException {
		locks.putIfAbsent(fileName, new ReentrantReadWriteLock());
		ReentrantReadWriteLock lock = locks.get(fileName);
		
        File f = new File(directoryPath + fileName);
        BufferedInputStream br = new BufferedInputStream(new FileInputStream(f));
		byte data[] = new byte[(int) (f.length())];
		
		lock.readLock().lock();
		br.read(data);
		lock.readLock().unlock();
		
		FileContent content = new FileContent(fileName);
		content.setData(new String(data));
		return content;
	}
    
    public boolean commit(long txnID, long numOfMsgs) throws MessageNotFoundException, IOException{
        Map<Long, byte[]> chunkMap = transactionFileMap.get(txnID);
        // check recieved messages are acceptable
        if (chunkMap.size() < numOfMsgs)
			throw new MessageNotFoundException();
		
		String fileName = activeTransactions.get(txnID);
		ArrayList<ReplicaServerReplicaServerInterface> slaveReplicas = fileSlaveReplicasMap.get(fileName);
        
		
		Collection<byte[]> write_col = chunkMap.values();
		
		ArrayList<byte[]> write_lis = new ArrayList<byte[]>(write_col) ;
		
		boolean writeSuccess = true;
		for (ReplicaServerReplicaServerInterface replica : slaveReplicas) {
            boolean sucess = replica.updateFile(fileName, write_lis);
            writeSuccess = writeSuccess && sucess;
		}
				
		locks.putIfAbsent(fileName, new ReentrantReadWriteLock());
		ReentrantReadWriteLock lock = locks.get(fileName);
        
        if(writeSuccess){
            lock.writeLock().lock();
            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(directoryPath + fileName, true));
            for (Iterator<byte[]> iterator = chunkMap.values().iterator(); iterator.hasNext();) 
                bw.write(iterator.next());
            bw.close();
            lock.writeLock().unlock();
        }
		activeTransactions.remove(txnID);
		transactionFileMap.remove(txnID);
		return writeSuccess;
    }

    public boolean updateFile(String fileName, ArrayList<byte[]> chunkData) throws IOException{
        BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(directoryPath + fileName, true));
        for (Iterator<byte[]> iterator = chunkData.iterator(); iterator.hasNext();)
        	bw.write(iterator.next());
        bw.close();
		return true;
    }

    public boolean abort(long txnID) throws RemoteException{
        activeTransactions.remove(txnID);
        transactionFileMap.remove(txnID);
        // success abort always as no update in done on the other replicas
        return true;
    }
    
	public void registerSlaves(String fileName, ArrayList<ReplicaLoc> slaveReplicas) throws AccessException, RemoteException, NotBoundException {
		ArrayList<ReplicaServerReplicaServerInterface> slaveReplicasStubs = new ArrayList<ReplicaServerReplicaServerInterface>();
        fileSlaveReplicasLocMap.put(fileName, slaveReplicas);	
		for (ReplicaLoc loc : slaveReplicas) {
			// if the current locations is this replica .. ignore
			if (loc.getName() == this.replicaLoc.getName())
				continue;
			  
            Registry registry = LocateRegistry.getRegistry(loc.getIp(), loc.getPort());
            ReplicaServerReplicaServerInterface stub = (ReplicaServerReplicaServerInterface) registry.lookup(loc.getName());
            slaveReplicasStubs.add(stub);
   	    }
        fileSlaveReplicasMap.put(fileName, slaveReplicasStubs);
	}

    public void run(){
        String replicaName = replicaLoc.getName();
        String replicaAdd = replicaLoc.getIp();
        int replicaPort = replicaLoc.getPort();
        System.setProperty("java.rmi.server.hostname", replicaAdd);
        try {
            Registry registry = LocateRegistry.createRegistry(replicaPort);
            registry.bind(replicaName, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String args[]) {
		ReplicaLoc r1 = new ReplicaLoc("Replica1", "127.0.0.1", 50000) ;
		ReplicaLoc r2 = new ReplicaLoc("Replica2", "127.0.0.1", 50001) ;
		ReplicaLoc r3 = new ReplicaLoc("Replica3", "127.0.0.1", 50002) ;

		/*	
		File replica1dir = new File("replica1dir") ;
		File replica2dir = new File("replica2dir") ;
		File replica3dir = new File("replica3dir") ;
		System.out.println(replica1dir.exists());
		System.out.println(replica2dir.exists());
		System.out.println(replica3dir.exists());
	 	*/
		
		
		ReplicaServer rs1 = null, rs2 = null , rs3 = null;
		try {
			rs1 = new ReplicaServer(r1, "replica1dir/");
			System.out.println(r1.getName() + " is now alive at ->  address : " + r1.getIp() + " port : " + r1.getPort());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			rs2 = new ReplicaServer(r2, "replica2dir/");
			System.out.println(r2.getName() + " is now alive at ->  address : " + r2.getIp() + " port : " + r2.getPort());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			rs3 = new ReplicaServer(r3, "replica3dir/");
			System.out.println(r3.getName() + " is now alive at ->  address : " + r3.getIp() + " port : " + r3.getPort());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			ArrayList<ReplicaLoc> slaves = new ArrayList<ReplicaLoc>();
			slaves.add(r2);
			slaves.add(r3);
			rs1.registerSlaves("test1.txt", slaves);
		} catch (RemoteException | NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
    }


}
