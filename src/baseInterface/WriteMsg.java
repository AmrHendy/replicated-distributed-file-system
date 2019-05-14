package baseInterface;

import java.io.Serializable;

public class WriteMsg  implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 12345L;
	private long transactionId;
	private long timeStamp;
	private ReplicaLoc loc;

	public WriteMsg(long transactionId,long timeStamp , ReplicaLoc loc){
		this.loc = loc;
		this.timeStamp = timeStamp;
		this.transactionId = transactionId;
	}

	public long getTransactionId() {
		return transactionId;
	}
	
	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public ReplicaLoc getLoc() {
		return loc;
	}
	
	public void setLoc(ReplicaLoc loc) {
		this.loc = loc;
	}
}
