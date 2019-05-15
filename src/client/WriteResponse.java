package client;

import baseInterface.ReplicaLoc;
import baseInterface.ReplicaServerClientInterface;

public class WriteResponse {

	private long transactionId;
	private long messageSeqNumber;
	private ReplicaServerClientInterface replicaServer;
	private String fileName;

	public WriteResponse(long transactionId, long messageSeqNumber, ReplicaServerClientInterface replicaServer, String fileName) {
		super();
		this.transactionId = transactionId;
		this.messageSeqNumber = messageSeqNumber;
		this.replicaServer = replicaServer;
		this.fileName = fileName;
	}
	

	public String getFileName() {
		return fileName;
	}


	public void setFileName(String fileName) {
		this.fileName = fileName;
	}


	public ReplicaServerClientInterface getReplicaServer() {
		return replicaServer;
	}

	public void setReplicaServer(ReplicaServerClientInterface replicaServer) {
		this.replicaServer = replicaServer;
	}

	public long getTransactionId() {
		return transactionId;
	}
	
	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}
	
	public long getMessageSeqNumber() {
		return messageSeqNumber;
	}
	
	public void setMessageSeqNumber(long messageSeqNumber) {
		this.messageSeqNumber = messageSeqNumber;
	}
	
}
