package baseInterface;

import java.io.Serializable;

public class ReplicaLoc implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private String ip;
	private Integer port;
	private boolean alive;
	
	public ReplicaLoc() {
		super();
	}

	public ReplicaLoc(String name, String ip, Integer port) {
		this.name = name;
		this.ip = ip;
		this.port = port;
		this.alive = true;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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

	public boolean getAlive() {
		return alive;
	}
	public void setAlive(boolean alive) {
		this.alive = alive;
	}
}
