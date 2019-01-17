package com.company.Illumio;

/*
  Auther: Melody Wang
  Date: 01/17/2019
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
    //private List<int[][][]> ipRangesTables; //ranges of IP address
    private List<TreeMap<Integer, Set<String>>> portIPsTables; // single ports table: Map<port, ListOfIps> sorted by port number default
    private List<IPwithPort[][][]> ipPortsTables;

    class IPwithPort {
        Set<Integer> ports;
        int ip;
        //boolean inRange;

        public IPwithPort() {
            ports = null;
            ip = -1;
          //  inRange = false;
        }
    }

    public Firewall(String path) {
        //creat tables, each table has 4 subtables(maps)
        pRangesTables = new ArrayList<HashMap<Integer, Integer>>();
        //ipRangesTables = new ArrayList<int[][][]>();

        portIPsTables = new ArrayList<TreeMap<Integer, Set<String>>>();
        ipPortsTables = new ArrayList<IPwithPort[][][]>();


        //create tables
        for(Index index : Index.values()) {
            pRangesTables.add(index.getI(), new HashMap<Integer, Integer>());
            portIPsTables.add(index.getI(), new TreeMap<Integer, Set<String>>());

            IPwithPort[][][] initialIpPortTable = new IPwithPort[256][256][256];
            ipPortsTables.add(init(initialIpPortTable));
        }

        readFile(path);

    }

    private IPwithPort[][][] init(IPwithPort[][][] initialIpPortTable) {
        for(int i = 0; i < 256; i++) {
            for(int j = 0; j < 256; j++) {
                for(int k = 0; k < 256; k++) {
                    initialIpPortTable[i][j][k] = new IPwithPort();
                }
            }
        }
        return initialIpPortTable;
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
                String port = inlineTokens[2];
                String ipAddress = inlineTokens[3];

                // if has port range or ip range
                if(inlineTokens[2].indexOf('-') != -1 ){
                    updatePortRangesTables(inlineTokens, i);

                } else if(inlineTokens[3].indexOf('-') != -1) {

                    updateIPRangesTables(inlineTokens, i);

                } else {
                //update the mapping of ip -- port; port -- ip without ranges
                    updateSingleTables(i, Integer.valueOf(port), ipAddress);
                }
                line = br.readLine();
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
            System.out.println("read file success");
        }
    }


    private int[] ipAddressParser(String ip) {
        int[] ipAddTokens = new int[4];
        String[] tokens = ip.split("\\.");

        for(int i = 0; i < 4; i++){
            ipAddTokens[i] = Integer.valueOf(tokens[i]);
        }
        return ipAddTokens;
    }

    private void updateSingleTables(int i, int port, String ip) {
        //upate the corresponding portIpTable
        TreeMap<Integer, Set<String>> portIpMap =  portIPsTables.get(i);
        if(portIpMap.containsKey(port)) {
            portIpMap.get(port).add(ip);
        } else {
            Set<String> ips = new HashSet<String>();
            ips.add(ip);
            portIpMap.put(port, ips);
        }

        //upate the corresponding ipPortTable
        IPwithPort[][][] ipPortMap = ipPortsTables.get(i);
        int[] ipTokens = ipAddressParser(ip);

        IPwithPort targetIP = ipPortMap[ipTokens[0]][ipTokens[1]][ipTokens[2]];
        targetIP.ip = ipTokens[3];
        if(targetIP.ports != null) {
            targetIP.ports.add(port);
        } else {
            targetIP.ports = new HashSet<Integer>();
            targetIP.ports.add(port);
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
        String startIP= inlineTokens[3].split("-")[0];
        String endIP = inlineTokens[3].split("-")[1];

        //System.out.println(startIP + ", " + endIP);

        String[] startIPTokens = startIP.split("\\.");
        String[] endIPTokens = endIP.split("\\.");
        int port = Integer.valueOf(inlineTokens[2]);

        //int[][][] ipRangesMap = ipRangesTables.get(i);
        IPwithPort[][][] ipPortMap = ipPortsTables.get(i);
        //1.2.3.10 - 3.0.5.10 ; user 1.4.4.128
        updateIPRangesMap(ipPortMap, startIPTokens, endIPTokens);

    }

    private void updateIPRangesMap(IPwithPort[][][] ipRangesMap, String[] startIPTokens, String[] endIPTokens) {
        int[] startRange = new int[4];
        int[] endRange = new int[4];

        for(int i = 0; i < 3; i++) {
            startRange[i] = Integer.valueOf(startIPTokens[i]);
            endRange[i] = Integer.valueOf(endIPTokens[i]);
        }
        markAllIpsWithinRange(startRange, endRange, ipRangesMap);
    }

    private void markAllIpsWithinRange(int[] startRange, int[] endRange,IPwithPort[][][] ipRangesMap) {
            //ip pattern: i.j.m.n

            int istart = startRange[0];
            int iend = endRange[0];

            int jstart = startRange[1];
            int jend = endRange[1];

            int mstart = startRange[2];
            int mend = endRange[2];

            int nstart = startRange[3];
            int nend = endRange[3];

            //ex. 1.5.6.1 - 1.7.1.255
            if(istart == iend) {
                if (jstart == jend) {
                    if (mstart == mend) {
                        if (nstart < nend) {
                            for (int n = nstart; n <= nend; n++)
                                fillFromIPToIP(istart,jstart,mstart,n,ipRangesMap);
                        }
                    } else if (mstart < mend) {
                        //1.1.1.1 - 1.1.5.10 = 1.1.1.1-1.1.1.255 +1.1.2.0-1.1.2.255+...+1.1.5.0-1.1.5.10
                        fillOnlyLastTwoParts(istart,jstart,mstart, mend, nend,ipRangesMap);
                    } else { // mstart > mend
                        return;
                    }
                } else if (jstart < jend) {
                    for(int j = jstart; j < jend; j++) {
                        for(int m = 0; m < 256; m++) {
                            for(int n = 0; n < 256; n++) {
                                fillFromIPToIP(istart,j,m,n,ipRangesMap);
                            }

                        }
                    }
                    //remaining in part j starts with jend
                    //1.5.5.5 - 1.6.1.1  -->
                    //1.5.5.5 - 1.5.255.255, 1.6.0.0 - 1.6.1.1 --->
                    fillOnlyLastTwoParts(istart,jstart,mstart, mend, nend,ipRangesMap);

                } else {
                   return;
                }
            } else if(istart < iend) {
                //1.5.5.5-2.1.1.1 --> 1.5.5.5-1.255.255.255 + 2.0.0.0-2.1.1.1
                for(int i = istart ; i < iend; i++) {
                    for(int j = jstart; j < 256; j++) {
                        for(int m = mstart; m < 256; m++) {
                            for(int n = nstart; n < 256;  n++) {
                                fillFromIPToIP(i,j,m,n,ipRangesMap);
                            }
                        }
                    }
                }
            }
    }

    private void fillOnlyLastTwoParts(int i, int j, int mstart, int mend, int nend, IPwithPort[][][] ipRangesMap) {
        //ex. 1.1 - 5.10 = (1.1-1.255 +2.0-2.255+ 3.0-3.255 + 4.0-4.255) + 5.0-5.10
        for (int m = mstart; m < mend; m++) {
            for (int n = 0; n < 256; n++) {
                fillFromIPToIP(i,j,m,n,ipRangesMap);
            }
        }
        //remaining in part m starts with mend: 5.0-5.10
        for (int n = 0; n <= nend; n++) {
            fillFromIPToIP(i,j,mend,n,ipRangesMap);
        }
    }

    private void fillFromIPToIP(int i, int j, int m, int n, IPwithPort[][][] map) {
            map[i][j][m].ip = n;
    }

    private int calIndex(String direction, String protocol) {
        char dir = Character.toUpperCase(direction.charAt(0));
        char prot = Character.toUpperCase(protocol.charAt(0));

        Index index = Index.valueOf(String.valueOf(dir+""+prot));
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
        IPwithPort[][][] ipPortMap = ipPortsTables.get(i);
        if(searchInIpRanges(ipPortMap, ip))
            return true;


        //3. search the single port table
        TreeMap<Integer, Set<String>> portIPsTable =  portIPsTables.get(i);
        if(searchByPort(portIPsTable, port))
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

    private boolean searchInIpRanges(IPwithPort[][][] ipRangesTable, String ip) {
        int[] ipTokens = ipAddressParser(ip);
        if(ipRangesTable[ipTokens[0]][ipTokens[1]][ipTokens[2]].ip != -1) {
            return true;
        }
        return false;
    }


    private boolean searchByPort(TreeMap<Integer, Set<String>> portIPsTable, int port) {
        if(portIPsTable.containsKey(port)) {
            return true;
        }

        return false;
    }
}
