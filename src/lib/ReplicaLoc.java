package lib;

import java.io.Serializable;

public class ReplicaLoc  implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Integer rid;
	private String ip;
	private Integer port;

	public ReplicaLoc(Integer rid, String ip, Integer port) {
		this.rid = rid;
		this.ip = ip;
		this.port = port;
	}
	public Integer getRID() {
		return rid;
	}
	public void setRID(Integer rid) {
		this.rid = rid;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}

	@Override
	public String toString() {
	    return String.format("RID: %d, ip: %s, port: %d", this.rid, this.ip, this.port);
	}
}
