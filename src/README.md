## Run
1. First, compile files:  
``` Make```

2. Run Server.java in order to launch the master server and the replica servers  
  ```  
  java -cp ".:../jars/jcommander-1.71.jar" Server -ip[ip-address] -port [port-number] -dir [dirpath]
  ```  
  Or to use the default values  
  ```  
  java -cp ".:../jars/jcommander-1.71.jar" Server
  ```
3. Run ClientLauncher.java in order to launch the clients  
  ```  
  java ClientLauncher
  ```
