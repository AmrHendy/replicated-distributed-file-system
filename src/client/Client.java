package client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import baseInterface.FileContent;
import baseInterface.MasterServerClientInterface;
import baseInterface.MessageNotFoundException;
import baseInterface.ReplicaLoc;
import baseInterface.ReplicaServerClientInterface;
import baseInterface.WriteMsg;

public class Client {
	
	private MasterServerClientInterface master;

	public Client() throws RemoteException, NotBoundException {
		master = gethandle();
	}
	
	public void read(String fileName) throws FileNotFoundException, RemoteException, IOException, NotBoundException{
		ReplicaLoc[] loc  = master.read(fileName);
		System.out.println(loc[0].getName());
		// conection with replca
		ReplicaServerClientInterface replicaServer = gethandle(loc[0]);
		FileContent fileContent = replicaServer.read(fileName);
		System.out.println("Content = " + fileContent.getData());
	}

	public WriteResponse write(FileContent file) throws RemoteException, IOException, NotBoundException, MessageNotFoundException{
		WriteMsg msg  = master.write(file);
		System.out.println(msg.getTimeStamp());
		ReplicaLoc replicaLoc = msg.getLoc();
		ReplicaServerClientInterface replicaServer = gethandle(replicaLoc);
		String allData = file.getData();
		
		// assuming chunk size of 16KB
		final int CHUNK_SIZE = 2048;
		long msgSeqNum = 0;
		
		FileContent content = new FileContent(file.getFileName()) ;
		for(int startIndex = 0; startIndex < allData.length(); startIndex += CHUNK_SIZE){	
			int endIndex = Math.min(startIndex + CHUNK_SIZE, allData.length());
			content.setData(allData.substring(startIndex, endIndex));
			replicaServer.write(msg.getTransactionId(), msgSeqNum, content);
			msgSeqNum++;
		}
		return new WriteResponse(msg.getTransactionId(), msgSeqNum, replicaServer) ;
	}
	
	public boolean commit(WriteResponse response) throws RemoteException, FileNotFoundException, MessageNotFoundException, IOException {
		boolean successCommit = response.getReplicaServer().commit(response.getTransactionId(), response.getMessageSeqNumber());
		if(successCommit){
			System.out.println("Successfull Write");
		}
		else{
			System.out.println("Unsuccessfull Write");
		}
		return successCommit ;
	}

	public boolean abort(WriteResponse response) throws RemoteException, FileNotFoundException, MessageNotFoundException, IOException {
		return response.getReplicaServer().abort(response.getTransactionId());
	}
	
	public MasterServerClientInterface gethandle() throws RemoteException, NotBoundException{
		String masterName = "masterServer";
		String masterAdd = "127.0.0.1";
		int masterPort = 54443;
		System.setProperty("java.rmi.server.hostname", masterAdd);
		Registry reg = LocateRegistry.getRegistry(masterAdd,masterPort);
		return (MasterServerClientInterface) reg.lookup(masterName);
	}

	public ReplicaServerClientInterface gethandle(ReplicaLoc primrayReplica) throws RemoteException, NotBoundException{
		String replicaName = primrayReplica.getName();
		String replicaAdd = primrayReplica.getIp();
		int replicaPort = primrayReplica.getPort();
		System.setProperty("java.rmi.server.hostname", replicaAdd);
		Registry reg = LocateRegistry.getRegistry(replicaAdd, replicaPort);
		return (ReplicaServerClientInterface) reg.lookup(replicaName);
	}

	public void executeTransaction(String transactionFilePath){
		File transactionFile = new File(transactionFilePath);
		Scanner sc = new Scanner(transactionFile);
		// ignore the first heading line
		String line = sc.nextLine();
		// read the transaction
		String allWriteContent = "";
		String fileName = null;
		while (sc.hasNextLine()) {
			line = sc.nextLine()
			String[] splited = line.split("\t");
			String operation = splited[0];
			if(fileName == null){
				fileName = splited[1];
			}
			else if(fileName != splited[1]){
				System.out.println("Can't handle multiple files in the same transaction");
				return;
			}
			if(operation.equals("read")){
				read(fileName);
			}
			else if(operation.equals("write")){
				allWriteContent += splited[2];
			}
			else if(operation.equals("commit") || operation.equals("abort")){
				FileContent fileContent = FileContent(fileName);
				fileContent.setData(allWriteContent);
				allWriteContent = "";
				WriteResponse response = write(fileContent);
				if(operation.equals("commit")){
					commit(response);	
				}
				else{
					abort(response);
				}
			}
			else{
				// ignore that line
			}
		}
		// not existing commit or abort, so we will abort the transaction
		if(!allWriteContent.isEmpty()){
			FileContent fileContent = FileContent(fileName);
			fileContent.setData(allWriteContent);
			WriteResponse response = write(fileContent);
			abort(response);
		}
	}

	public static void main(String[] args) throws NotBoundException, FileNotFoundException, IOException {
		
		/*
		 * test read write not found file commit
		 * 	
		Client c = new Client();
		c.read("test1.txt");
	
		FileContent f = new FileContent("test1.txt");
		f.setData("write is done 1");
		
		try {
			c.commit(c.write(f));
		} catch (MessageNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		c.read("test1.txt");
	
 		*/
		
		/*
		 * test read write not found file abort 
		 * 
		Client c = new Client();
		FileContent f = new FileContent("test6.txt");
		f.setData("write is done 1");
		
		try {
			c.abort(c.write(f));
		} catch (MessageNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		*/	
		
		
		/*
		 * test read write not found file commit
		 * 
		Client c = new Client();
		FileContent f = new FileContent("test6.txt");
		f.setData("write is done 1");
		
		try {
			c.commit(c.write(f));
		} catch (MessageNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		
		/*
		 * multiple writes at same time 
		 * 
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Client c1 = new Client();
					FileContent f = new FileContent("test7.txt");
					f.setData("write is done 1");
					Thread.sleep(1000);
					WriteResponse response = c1.write(f);
					Thread.sleep(1000);
					c1.commit(response);
				} catch (InterruptedException | NotBoundException | IOException | MessageNotFoundException e) {
					e.printStackTrace();
				}
			}
		});
		
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Client c1 = new Client();
					FileContent f = new FileContent("test7.txt");
					f.setData("write is done 2");
					Thread.sleep(1000);
					WriteResponse response = c1.write(f);
					Thread.sleep(1000);
					c1.commit(response);
				} catch (InterruptedException | NotBoundException | IOException | MessageNotFoundException e) {
					e.printStackTrace();
				}
			}
		});
		
		Thread t3 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Client c1 = new Client();
					FileContent f = new FileContent("test7.txt");
					f.setData("write is done 3");
					Thread.sleep(1000);
					WriteResponse response = c1.write(f);
					Thread.sleep(1000);
					c1.commit(response);
				} catch (InterruptedException | NotBoundException | IOException | MessageNotFoundException e) {
					e.printStackTrace();
				}
			}
		});
		t1.start();
		t2.start();
		t3.start();
		*/
	
	}
}
