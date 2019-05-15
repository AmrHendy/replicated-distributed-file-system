package baseInterface;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface MasterServerReplicaServerInterface extends Remote {

	public void registerSlaves(String fileName, ArrayList<ReplicaLoc> slaveReplicas) throws AccessException, RemoteException, NotBoundException;
	
	public boolean checkAlive() throws RemoteException;
}
