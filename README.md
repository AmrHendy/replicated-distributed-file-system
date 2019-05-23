# Replicated Distributed File System

## Overview
This is an implementation of a replicated file system. There will be one main server (master) and, data will be replicated on multiple replicaServers. This file system allows its concurrent users to perform transactions, while guaranteeing ACID properties. This means that the following need to be ensured:  
  - The master server maintains metadata about the replicas and their locations.
  - The user can submit multiple operations that are performed atomically on the shared files stored in the distributed file system.
  - Files are not partitioned.
  - Assumption: each transaction access only one file.
  - Each file stored on the distributed file system has a primary replica. This means that you will need to implement sequential consistency through a protocol similar to the passive (primary-backup) replication protocol.
  - After performing a set of operations, it is guaranteed that the file system will remain in consistent state.
  - A lock manager is maintained at each replicaServer.
  - Once any transaction is committed, its mutations to files stored in the file system are required to be durable.

## Characteristics of the Distributed File System
1. **Reads from the file system**. Files are read entirely. When a client sends a file name to the primary replicaServer, the entire file is returned to the client. The client initially contacts the master node to get the IP of the replicaServer acting as primary for the file that it wants to read.

2. **Writes to files stored on the file system**. This procedure is followed:
  - The client requests a new transaction ID from the server. The request includes the name of the file to be mutated during the transaction.  
  - The server generates a unique transaction ID and returns it, a timestamp, and the location of the primary replica of that le to the client in an acknowledgment to the clients file update request.  
  - If the file specified by the client does not exist, the master creates metadata for the file and chooses where its replicas can be located and which one of them will be the primary. The client will then communicate these information to the primary along with the write request. Selecting the replicaServers to place the replicas of the file and selecting of the primary replica can be random.
  - All subsequent client messages corresponding to a transaction will be directed to the replicaServer with the primary replica contain the ID of that transaction.
  - The client sends to the replicaServer a series of write requests to the file specified in the transaction. Each request has a unique serial number. The server appends all writes sent by the client to the file. Updates are also propagated in the same order to other replicaServers.
  - The replicaServer with the primary replica must keep track of all messages received from the client as part of each transaction. The server must also apply file mutations based on the correct order of the transactions.
  - At the end of the transaction, the client issues a commit request. This request guarantees that the file is written on all the replicaServer disks. Therefore, each replicaServer flushes the file data to disk and sends an acknowledgement of the committed transaction to the primary replicaServer for that file. Once the primary replicaServer receives acknowledgements from all replicas, it sends an acknowledgement to the client.
  - The new file must not be seen on the file system until the transaction commits. That is a read request to a file that is being updated by an uncommitted transaction must generate an error.

3. **Client specification.**
  - Clients read and write data to the distributed file system.
  - Each client has a file in its local directory that specifies the main server IP address.

4. **Master Server specification**.
  - The master server should communicate with clients through the given RMI interface.
  - The master server need to be invoked using the following command.
  ```
  server -ip [ip address string] -port [port number] -dir <directory path>
  ```
  where:
  - **ip address** string is the ip address of the server. The default value is
  127.0.0.1.
  - **port number** is the port number at which the server will be listening to messages. The default is 8080.
  - Note: you can start the replicaServers similar to the master server, or the master server can start them while it is starting

5. **ReplicaServer specification**.
  - Each file is replicated on three replicaServers. The IP addresses of all             replicaServers are specified in a file called repServers.txt. The master keeps       heartbeats with these servers.
  - Acknowledgements are sent to the client when data is written to all the             replicaServers that contain replicas of a certain le.
  - You will need to implement a similar protocol to the passive (primary-backup)       replication protocol studied in class.
  - When the client asks the primary replicaServer to commit a transaction, the         primary replica must ensure that the transaction data is flushed to disk on the     all the replicaServers local file systems before committing the transaction.  
    Using the write is not enough to accomplish this because it puts the data into      file system buffer cache, but does not force it to disk. Therefore, you must        explicitly flush the data to disk when committing client transactions or when        writing important housekeeping messages that must survive crashes. To flush the      data to disk, use flush() and close() methods of the FileWriter in Java.

6. **Concurrency.**  
Multiple clients may be executing transactions against the same file. Isolation and concurrency control properties should be guaranteed by the server..

## Contributers:
1. [Amr Hendy](https://github.com/AmrHendy) 
2. [Abdelrahman Yasser](https://github.com/Abdelrhman-Yasser)
3. [Hossam Fawzy](https://github.com/hosamelsafty)
4. [Mohammed Shaban](https://github.com/mohamed-shaapan)
