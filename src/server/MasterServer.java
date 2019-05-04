package server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;

import baseInterface.FileContent;
import baseInterface.MasterServerClientInterface;
import baseInterface.ReplicaLoc;
import baseInterface.WriteMsg;

public class MasterServer implements MasterServerClientInterface{

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public ReplicaLoc[] read(String fileName) throws FileNotFoundException, IOException, RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WriteMsg write(FileContent data) throws RemoteException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
