package server;

public class ServerMain{
	public static void main(String[] args) throws IOException {
        // run the master server
        // it will automaticaly bind the servermaster and waiting for communication from the client.
        // it will automaticaly start the replicas servers running and waiting for communication.
        MasterServer masterServer = new MasterServer();	        
    }
}
