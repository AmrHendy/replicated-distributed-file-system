package baseInterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MasterServerReplicaServerInterface extends Remote {

	public void registerSlaves(String fileName, List<ReplicaLoc> slaveReplicas) throws AccessException, RemoteException, NotBoundException;
}
