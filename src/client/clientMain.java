package client;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import baseInterface.MessageNotFoundException;

public class clientMain{
	public static void main(String[] args) {
        // test using transaction txt files numbered from 1 to testNumber
        int testNumber = 1;
        for(int test = 1; test <= testNumber; test++){
            String transactionFilePath = "tests/transaction" + test + ".txt";
            Thread clientThread = new Thread(new Runnable() {
				@Override
				public void run() {
                    // sleep to simulate randomness in reality
                    long minSleep = 1000;
		            long maxSleep = 5000;
		            long sleepTime = minSleep + (long)(Math.random() * (maxSleep - minSleep));
                    try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    // execute transaction
                    Logger.getLogger("client").log(Level.INFO,"Start Client executing " + transactionFilePath);
                    Client client;
					try {
						client = new Client();
						client.executeTransaction(transactionFilePath);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NotBoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (MessageNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
            });
            clientThread.start();
        }	        
    }
}
