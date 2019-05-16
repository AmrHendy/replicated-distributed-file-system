package baseInterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MasterServerClientInterface extends Remote {
	/**
	 * Read file from server
	 * 
	 * @param fileName
	 * @return the addresses of  of its different replicas
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws RemoteException
	 * @throws MessageNotFoundException 
	 */
	public ReplicaLoc[] read(String fileName) throws RemoteException,FileNotFoundException, MessageNotFoundException;

	/**
	 * Start a new write transaction
	 * 
	 * @param fileName
	 * @return the requiref info
	 * @throws RemoteException
	 * @throws IOException
	 * @throws NotBoundException 
	 * @throws MessageNotFoundException 
	 */
	public WriteMsg write(FileContent data) throws RemoteException, NotBoundException, MessageNotFoundException;

}
