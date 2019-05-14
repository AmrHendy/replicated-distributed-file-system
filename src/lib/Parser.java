package lib;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Parser {
    private static final String SEP = " ";

    public ConcurrentHashMap<Integer, ReplicaLoc> parse(String replicaInfoFile) {
        Path filePath = Paths.get(replicaInfoFile);
        ConcurrentHashMap<Integer, ReplicaLoc> map = null;
        Scanner scanner = null;
        try {
            scanner = new Scanner(filePath);
            map = new ConcurrentHashMap<>();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(SEP);
                if (parts.length != 0) {
                    Integer rid = Integer.parseInt(parts[0]);
                    String ip = parts[1];
                    Integer port = Integer.parseInt(parts[2]);
                    map.put(rid, new ReplicaLoc(rid, ip, port));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(map != null){
            for (Map.Entry<Integer, ReplicaLoc> entry : map.entrySet()) {
                System.out.println(entry.getValue());
            }
        }
        return map;
    }
}