package baseInterface;

import java.io.Serializable;

public class WriteAck implements Serializable {

	private static final long serialVersionUID = 112233L;
	private long transactionId;
	private long messageSeqNumber;

	public WriteAck(long transactionId, long messageSeqNumber){
		this.transactionId = transactionId;
		this.messageSeqNumber = messageSeqNumber;
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
