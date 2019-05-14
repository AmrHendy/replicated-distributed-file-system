package replica.server;

import baseInterface.FileContent;
import baseInterface.ReplicaServerClientInterface;
import baseInterface.ReplicaLoc;
import baseInterface.WriteMsg;

public class ReplicaServer extends UnicastRemoteObject implements ReplicaServerClientInterface{

    private ReplicaLoc replicaLoc;
    private String directoryPath;
    private ConcurrentMap<String, ReentrantReadWriteLock> locks; // locks objects of the open files
    
    // write data
    private Map<Long, String> activeTransactions; // map between active transactions and file names
	private Map<Long, Map<Long, byte[]>> transactionFileMap; // map between transaction ID and corresponding file chunks


    public ReplicaServer(ReplicaLoc replicaLoc, String directoryPath){
        this.replicaLoc = replicaLoc;
        this.directoryPath = directoryPath;
        this.locs = new ConcurrentMap<>();
        this.run();
    }


	public WriteMsg write(long txnID, long msgSeqNum, FileContent data) throws RemoteException, IOException{
        // if this is not the first message of the write transaction
        if (!transactionFileMap.containsKey(txnID)){
            transactionFileMap.put(txnID, new TreeMap<Long, byte[]>());
            activeTransactions.put(txnID, data.getFileName());
        }
        Map<Long, byte[]> chunkMap =  transactionFileMap.get(txnID);
        chunkMap.put(msgSeqNum, data.getData());
        return new ChunkAck(txnID, msgSeqNum);
    }


    @Override
	public FileContent read(String fileName) throws FileNotFoundException, RemoteException, IOException {
		File f = new File(directoryPath + fileName);
		
		locks.putIfAbsent(fileName, new ReentrantReadWriteLock());
		ReentrantReadWriteLock lock = locks.get(fileName);
		
		BufferedInputStream br = new BufferedInputStream(new FileInputStream(f));
		
		// assuming files are small and can fit in memory
		byte data[] = new byte[(int) (f.length())];
		
		lock.readLock().lock();
		br.read(data);
		lock.readLock().unlock();
		
		FileContent content = new FileContent(fileName, data);
		return content;
	}

    
    public boolean commit(long txnID, long numOfMsgs)
        throws MessageNotFoundException, RemoteException;

    
    public boolean abort(long txnID) throws RemoteException{
        activeTransactions.remove(txnID);
        transactionFileMap.remove(txnID);
        return false;
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
