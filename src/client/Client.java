package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import baseInterface.FileContent;
import baseInterface.MasterServerClientInterface;
import baseInterface.MessageNotFoundException;
import baseInterface.ReplicaLoc;
import baseInterface.ReplicaServerClientInterface;
import baseInterface.WriteMsg;

public class Client {
	
	private static final String MASTER_METADATA = "masterServer.txt";
	private MasterServerClientInterface master;
	private static Logger logger;
	private static String log_name = "client";
	
	
	public Client(){
		try {
			master = gethandle();
		} catch (RemoteException e) {
			Logger.getLogger(log_name).log(Level.SEVERE,"Master server is down");
			System.exit(-1);
		} catch (NotBoundException e) {
			Logger.getLogger(log_name).log(Level.SEVERE,"Master server is down");
			System.exit(-1);
		}
		logger = Logger.getLogger(this.getClass().getName());
	}
	
	public void read(String fileName){
		ReplicaLoc[] loc = null;
		try {
			Logger.getLogger(log_name).log(Level.INFO,"Client request replicas location for read");
			loc = master.read(fileName);
		} catch (FileNotFoundException e) {
			logger.log(Level.WARNING,"File " + fileName  + " to be read is not found in ditributed file system");
			return;
		} catch (RemoteException e) {
			Logger.getLogger(log_name).log(Level.SEVERE,"Master server is down");
			System.exit(-1);
		} catch (MessageNotFoundException e) {
			logger.log(Level.WARNING,"All replicas is down");
			return;
		}
		
		// conection with replca
		ReplicaServerClientInterface replicaServer = null;
		
		try {
			Logger.getLogger(log_name).log(Level.INFO,"Client is connecting with replica " + loc[0].getName());
			replicaServer = gethandle(loc[0]);
		} catch (RemoteException e1) {
			logger.log(Level.SEVERE,"Master replica server is down before reading");
			System.exit(-1);
		} catch (NotBoundException e1) {
			logger.log(Level.SEVERE,"Stub was not found at replica server");
			System.exit(-1);
		}
		
		FileContent fileContent = null;
		try {
			Logger.getLogger(log_name).log(Level.INFO,"Client request read from replica");
			fileContent = replicaServer.read(fileName);
			Logger.getLogger(log_name).log(Level.INFO,"Client request read return with : " + fileContent.getData());
		} catch (FileNotFoundException e) {
			logger.log(Level.WARNING,"File to be read is not found in ditributed file system");
			return;
		} catch (RemoteException e) {
			logger.log(Level.SEVERE,"Master replica crashed before reading");
			System.exit(-1);
		} catch (IOException e) {
			logger.log(Level.WARNING,"IO exception occurs at replica during reading");
			return;
		}
		
		
		
	}

	public WriteResponse write(FileContent file){
		
		WriteMsg msg = null;
		try {
			Logger.getLogger(log_name).log(Level.INFO,"Client request replicas location for write");
			msg = master.write(file);
		} catch (RemoteException e) {
			logger.log(Level.SEVERE,"Master server is down");
			System.exit(-1);
		} catch (NotBoundException e) {
			logger.log(Level.SEVERE,"Communications between Master and Client is broken");
			System.exit(-1);
		} catch (MessageNotFoundException e) {
			logger.log(Level.SEVERE,"All replicas crashed");
			System.exit(-1);
		}
		
		ReplicaLoc replicaLoc = msg.getLoc();
		ReplicaServerClientInterface replicaServer = null;
		try {
			Logger.getLogger(log_name).log(Level.INFO,"Client is connecting with replica " + replicaLoc.getName());
			replicaServer = gethandle(replicaLoc);
		} catch (RemoteException e1) {
			logger.log(Level.SEVERE,"Master replica server is down before writing");
			System.exit(-1);
		} catch (NotBoundException e1) {
			logger.log(Level.SEVERE,"Stub was not found at replica server");
			System.exit(-1);
		}
		
		String allData = file.getData();
		
		// assuming chunk size of 16KB
		final int CHUNK_SIZE = 2048;
		long msgSeqNum = 0;
		
		FileContent content = new FileContent(file.getFileName()) ;
		for(int startIndex = 0; startIndex < allData.length(); startIndex += CHUNK_SIZE){	
			int endIndex = Math.min(startIndex + CHUNK_SIZE, allData.length());
			content.setData(allData.substring(startIndex, endIndex));
			try {
				Logger.getLogger(log_name).log(Level.INFO,"Client request write chunk__" + msgSeqNum + " to replica");
				replicaServer.write(msg.getTransactionId(), msgSeqNum, content);
			} catch (RemoteException e) {
				logger.log(Level.SEVERE,"Master replica crashed before writing chunk : " + msgSeqNum);
				System.exit(-1);
			} catch (IOException e) {
				logger.log(Level.SEVERE,"IO exception occurs at replica during writing chunk : " + msgSeqNum);
				System.exit(-1);
			}
			msgSeqNum++;
		}
		return new WriteResponse(msg.getTransactionId(), msgSeqNum, replicaServer, file.getFileName()) ;
	}
	
	public boolean commit(WriteResponse response){
		boolean successCommit = false;
		
		try {
			Logger.getLogger(log_name).log(Level.INFO,"Client request commit on file " + 
					response.getFileName() + " on transaction " + response.getTransactionId() + " to replica" );
			successCommit = response.getReplicaServer().commit(response.getTransactionId(), response.getMessageSeqNumber());
		} catch (RemoteException e) {
			logger.log(Level.SEVERE,"Master replica server is down before commiting");
			System.exit(-1);
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE,"File to be commited is not found in ditributed file system");
			System.exit(-1);
		} catch (MessageNotFoundException e) {
			logger.log(Level.SEVERE,"Error occurs during writing file");
			System.exit(-1);
		} catch (IOException e) {
			logger.log(Level.SEVERE,"IO exception occurs at replica during commiting");
			System.exit(-1);
		}
		
		if(successCommit){
			logger.log(Level.INFO,"Successful commiting for file : " + response.getFileName());
		}
		else{
			logger.log(Level.INFO,"Unsuccessful commiting for file : " + response.getFileName());
		}
		
		return successCommit ;
	}

	public boolean abort(WriteResponse response){
		try {
			Logger.getLogger(log_name).log(Level.INFO,"Client request abort on file " + 
					response.getFileName() + " on transaction " + response.getTransactionId() + " to replica" );
			return response.getReplicaServer().abort(response.getTransactionId());
		} catch (RemoteException e) {
			logger.log(Level.SEVERE,"Master replica server is down before commiting");
			System.exit(-1);
		}
		return false;
	}
	
	public MasterServerClientInterface gethandle() throws RemoteException, NotBoundException{
		File masterServerFile = new File(MASTER_METADATA);
		Scanner sc = null;
		
		try {
			sc = new Scanner(masterServerFile);
		} catch (FileNotFoundException e) {
			Logger.getLogger(log_name).log(Level.WARNING, "Master configuration file is not found");
			System.exit(-1);
		}
		
		// ignore the first heading line
		String line = sc.nextLine();
		// read the master server information 
		line = sc.nextLine();
		String[] splited = line.split(" ");
		String masterName = splited[0];
		String masterAdd = splited[1];
		int masterPort = Integer.parseInt(splited[2]);
		
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

	public void executeTransaction(String transactionFilePath) throws NotBoundException, IOException, MessageNotFoundException{
		File transactionFile = new File(transactionFilePath);
		
		Scanner sc = null;
		
		try {
			sc = new Scanner(transactionFile);
		} catch (FileNotFoundException e) {
			Logger.getLogger(log_name).log(Level.WARNING,"Transaction file " + transactionFilePath  +  " is not found");
			return;
		}
		
		// ignore the first heading line
		String line = sc.nextLine();
		// read the transaction
		String allWriteContent = "";
		String fileName = null;
		while (sc.hasNextLine()) {
			line = sc.nextLine();
			String[] splited = line.split("\t");
			String operation = splited[0];
			if(fileName == null){
				fileName = splited[1];
			}
			else if(!fileName.equals(splited[1])){
				Logger.getLogger(log_name).log(Level.WARNING,"Can't handle multiple files in the same transaction ");
				return;
			}
			if(operation.equals("read")){
				read(fileName);
			}
			else if(operation.equals("write")){
				allWriteContent += splited[2];
			}
			else if(operation.equals("commit") || operation.equals("abort")){
				FileContent fileContent = new FileContent(fileName);
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
			FileContent fileContent = new FileContent(fileName);
			fileContent.setData(allWriteContent);
			WriteResponse response = write(fileContent);
			abort(response);
		}
	}
	
}
