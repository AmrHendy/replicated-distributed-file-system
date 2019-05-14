// Exceptions
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.IOException;
import lib.MessageNotFoundException;

import java.util.List;
import java.util.ArrayList;
import client.Client;


public class ClientLauncher {

	private static void launchClients() throws AccessException, RemoteException{
		try{
			Client c1 = new Client();
			// read text file
			c1.read("text1.txt");

		} catch (NotBoundException | IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception{
		// Set up clients
		launchClients();
	}
}