package replica.server;

import baseInterface.FileContent;
import baseInterface.ReplicaServerClientInterface;
import baseInterface.ReplicaLoc;
import baseInterface.WriteMsg;

public class ReplicaServer extends UnicastRemoteObject implements ReplicaServerClientInterface, ReplicaServerReplicaServerInterface, MasterServerReplicaServerInterface{

    private ReplicaLoc replicaLoc;
    private String directoryPath;
    private ConcurrentMap<String, ReentrantReadWriteLock> locks; // locks objects of the open files
    private ConcurrentMap<String, List<ReplicaServerReplicaServerInterface>> fileSlaveReplicasMap;
    private ConcurrentMap<String, List<ReplicaLoc>> fileSlaveReplicasLocMap;
    
    // write data
    private Map<Long, String> activeTransactions; // map between active transactions and file names
	private Map<Long, Map<Long, byte[]>> transactionFileMap; // map between transaction ID and corresponding file chunks

    public ReplicaServer(ReplicaLoc replicaLoc, String directoryPath){
        this.replicaLoc = replicaLoc;
        this.directoryPath = directoryPath;
        this.locs = new ConcurrentMap<>();
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
        chunkMap.put(msgSeqNum, data.getData());
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
		
		FileContent content = new FileContent(fileName, data);
		return content;
	}
    
    public boolean commit(long txnID, long numOfMsgs) throws MessageNotFoundException, RemoteException{
        Map<Long, byte[]> chunkMap = transactionFileMap.get(txnID);
        // check recieved messages are acceptable
        if (chunkMap.size() < numOfMsgs)
			throw new MessageNotFoundException();
		
		String fileName = activeTransactions.get(txnID);
		List<ReplicaServerReplicaServerInterface> slaveReplicas = fileSlaveReplicasMap.get(fileName);
        
        boolean writeSuccess = true;
		for (ReplicaServerReplicaServerInterface replica : slaveReplicasStubs) {
            boolean sucess = replica.updateFile(fileName, new ArrayList<>(chunkMap.values()));
            writeSuccess = writeSuccess && success;
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

    public boolean updateFile(String fileName, ArrayList<Byte[]> chunkData){
        BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(directoryPath + fileName, true));
        for (Iterator<byte[]> iterator = chunkData.iterator(); iterator.hasNext();){
            bw.write(iterator.next());
        } 
        bw.close();
    }

    public boolean abort(long txnID) throws RemoteException{
        activeTransactions.remove(txnID);
        transactionFileMap.remove(txnID);
        // success abort always as no update in done on the other replicas
        return true;
    }
    
	public void registerSlaves(String fileName, List<ReplicaLoc> slaveReplicas) throws AccessException, RemoteException, NotBoundException {
        List<ReplicaReplicaInterface> slaveReplicasStubs = new ArrayList<ReplicaReplicaInterface>();
        fileSlaveReplicasLocMap.put(fileName, slaveReplicas);	
		for (ReplicaLoc loc : slaveReplicas) {
			// if the current locations is this replica .. ignore
			if (loc.getName() == this.replicaLoc.getName())
				continue;
			  
            Registry registry = LocateRegistry.getRegistry(loc.getIp(), loc.getPort());
            ReplicaServerReplicaServerInterface stub = (ReplicaServerReplicaServerInterface) registry.lookup(loc.getName()));
            slaveReplicasStubs.add(stub);
   	    }
		filesReplicaMap.put(fileName, slaveReplicasStubs);
	}

    public void run(){
        String replicaName = replicaLoc.getName();
        String replicaAdd = replicaLoc.getIp();
        int replicaPort = replicaLoc.getIp();
        System.setProperty("java.rmi.server.hostname", replicaAdd);
        try {
            Registry registry = LocateRegistry.createRegistry(this);
            registry.bind(replicaName, replica);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
