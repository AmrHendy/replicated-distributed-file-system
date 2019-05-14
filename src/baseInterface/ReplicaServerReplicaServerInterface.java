package baseInterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ReplicaServerReplicaServerInterface extends Remote {
    
    public boolean updateFile(String fileName, ArrayList<Byte[]> chunkData) throws RemoteException, IOException;
}
