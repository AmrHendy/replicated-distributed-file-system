package client;

public class clientMain{
	public static void main(String[] args) throws IOException {
        // test using transaction txt files numbered from 1 to testNumber
        int testNumber = 1;
        for(int test = 1; test <= testNumber; test++){
            String transactionFilePath = "/tests/transaction" + test + ".txt";
            Thread clientThread = new Thread(new Runnable() {
				@Override
				public void run() {
                    // sleep to simulate randomness in reality
                    long minSleep = 1000;
		            long maxSleep = 5000;
		            long sleepTime = minSleep + (long)(Math.random() * (maxSleep - minSleep));
                    Thread.sleep(sleepTime);
                    // execute transaction
                    System.out.println("Start Client executing " + transactionFilePath);
                    Client client = new Client()
                    client.executeTransaction(transactionFilePath);
				}
            });
            clientThread.start();
        }	        
    }
}
