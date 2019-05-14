package baseInterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

//import test.MessageNotFoundException;

public interface ReplicaServerClientInterface extends Remote {
	/**
	 * 
	 * @param txnID
	 *            : the ID of the transaction to which this message relates
	 * @param msgSeqNum
	 *            : the message sequence number. Each transaction starts with
	 *            message sequence number 1.
	 * @param data
	 *            : data to write in the file
	 * @return message with required info
	 * @throws IOException
	 * @throws RemoteException
	 */
	public WriteAck write(long txnID, long msgSeqNum, FileContent data)
			throws RemoteException, IOException;
	
	public FileContent read(String fileName) throws FileNotFoundException,
	IOException, RemoteException;
	
	/**
	 * 
	 * @param txnID
	 *            : the ID of the transaction to which this message relates
	 * @param numOfMsgs
	 *            : Number of messages sent to the server
	 * @return true for acknowledgment
	 * @throws MessageNotFoundException
	 * @throws RemoteException
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	public boolean commit(long txnID, long numOfMsgs)
			throws MessageNotFoundException, RemoteException, FileNotFoundException, IOException;
	
	/**
	 * * @param txnID: the ID of the transaction to which this message relates
	 * 
	 * @return true for acknowledgment
	 * @throws RemoteException
	 */
	public boolean abort(long txnID) throws RemoteException;
}
