package baseInterface;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface ReplicaServerReplicaServerInterface extends Remote {
    
    public boolean updateFile(String fileName, ArrayList<byte[]> chunkData) throws RemoteException, IOException;
}
