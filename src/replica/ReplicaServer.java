package replica;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import baseInterface.ReplicaServerClientInterface;
import lib.FileContent;
import lib.ReplicaLoc;
import lib.WriteMsg;
import lib.Parser;


public class ReplicaServer extends UnicastRemoteObject implements ReplicaServerClientInterface{
	/**
	 *
	*/

}
