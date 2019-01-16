package com.company.Illumio;

/*
  Auther: Melody Wang
  Date: 01/16/2019
  Applied Position: Backend Internship 2019
*/
import java.io.*;
import java.util.*;

enum Index {
    IT (0),   //Inbound + TCP : i= 0,
    IU (1),   //Inbound + UDP : i= 1,
    OT (2),
    OU (3);

    private int i;

    private Index(int i) {
        this.i = i;
    }
    public int getI() {
        return this.i;
    }

}

public class Firewall {
    private File rules;

    private List<HashMap<Integer, Integer>> pRangesTables; //ranges of port <startPort, endPoirt>, sorted by startPort
    private List<HashMap<int[][], int[][]>> ipRangesTables; //ranges of IP address

    private List<TreeMap<Integer, Set<String>>> portIPsTables; // single ports table: Map<port, ListOfIps> sorted by port number default
    private List<HashMap<String, Set<Integer>>> ipPortsTables; // single ips table: Map<Ip,SetOfPorts> sorted by IP


    public Firewall(String path) {
        pRangesTables = new ArrayList<HashMap<Integer, Integer>>();
        ipRangesTables = new ArrayList<HashMap<int[][], int[][]>>();

        portIPsTables = new ArrayList<TreeMap<Integer, Set<String>>>();
        ipPortsTables = new ArrayList<HashMap<String, Set<Integer>>>();

        readFile(path);

    }

    private void readFile(String path) {
        rules = new File(path);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(rules));
            String[] tokens = null;
            String line = br.readLine();

            while(line != null) {
                String[] inlineTokens = line.split(",");
                int i = calIndex(inlineTokens[0], inlineTokens[1]);
                int port = Integer.valueOf(inlineTokens[2]);
                String ipAddress = inlineTokens[4];

                // if has port range or ip range
                if(inlineTokens[2].indexOf('-') != -1 || inlineTokens[3].indexOf('-') != -1) {
                    updatePortRangesTables(inlineTokens, i);
                    updateIPRangesTables(inlineTokens, i);

                } else {
                    //update the mapping of ip -- port; port -- ip without ranges
                    updateSingleTables(i, port, ipAddress);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void updateSingleTables(int i, int port, String ip) {
        TreeMap<Integer, Set<String>> portIpMap =  portIPsTables.get(i);
        if(portIpMap.containsKey(port)) {
            portIpMap.get(port).add(ip);
        }

        HashMap<String, Set<Integer>> ipPortMap = ipPortsTables.get(i);
        if(ipPortMap.containsKey(ip)) {
            ipPortMap.get(ip).add(port);
        }
    }

    private void updatePortRangesTables(String[] inlineTokens, int i) {
        String startPort = inlineTokens[2].split("-")[0];
        String endPort = inlineTokens[2].split("-")[1];

        int key = (int)Integer.valueOf(startPort);
        int curEndPort = (int)Integer.valueOf(endPort);

        HashMap<Integer, Integer> portRangesMap = pRangesTables.get(i);

        if(portRangesMap.containsKey(key)) {
            if (curEndPort < portRangesMap.get(key)) {
                return;
            }
        }
        portRangesMap.put(key, curEndPort); //put or update existed <startport, endport>
    }


    private void updateIPRangesTables(String[] inlineTokens, int i) {
        //todo
    }

    private int calIndex(String direction, String protocol) {
        char dir = Character.toUpperCase(direction.charAt(0));
        char prot = Character.toUpperCase(protocol.charAt(0));

        Index index = Index.valueOf(String.valueOf(dir+prot));
        int i = index.getI();
        return i;
    }

    public boolean accept_packet(String direction, String protocol, int port, String ip) {
        int i = calIndex(direction, protocol);

        //1. go through pRangesTable, if port in range of any <key, value>, return true
        HashMap<Integer, Integer> pRangesTable = pRangesTables.get(i);
        if(searchInPortRanges(pRangesTable, port))
            return true;


        //2. go through ipRangesTable, if ip in range of any <key, value>, return true
        HashMap<int[][], int[][]> ipRangesTable = ipRangesTables.get(i);
        if(searchInIpRanges(ipRangesTable, ip))
            return true;


        //3. search the single port table
        TreeMap<Integer, Set<String>> portIPsTable =  portIPsTables.get(i);
        if(searchByPort(portIPsTable, port))
            return true;

        //else search the single IP table
        HashMap<String, Set<Integer>> ipPortsTable = ipPortsTables.get(i);
        if(searchByIP(ipPortsTable, ip))
            return true;


        //otherwise not allowed
        return false;

    }

    private boolean searchInPortRanges(HashMap<Integer, Integer> pRangesTable, int port) {
        for( Map.Entry<Integer, Integer> entry : pRangesTable.entrySet()) {
            int startPort = entry.getKey();
            int endPort = entry.getValue();

            if(port == startPort || port == endPort
                    || startPort < port && port < endPort) {
                return true;
            } else if (port < startPort) {
                break;
            }
        }

        return false;
    }

    private boolean searchInIpRanges(HashMap<int[][], int[][]> ipRangesTable, String ip) {
        //todo
        return false;
    }


    private boolean searchByPort(TreeMap<Integer, Set<String>> portIPsTable, int port) {
        if(portIPsTable.containsKey(port)) {
            return true;
        }

        return false;
    }

    private boolean searchByIP(HashMap<String, Set<Integer>> IpPortsTable, String ip) {
        if(IpPortsTable.containsKey(ip)) {
            return true;
        }
        return false;
    }
}
