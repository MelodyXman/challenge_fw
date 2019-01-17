package com.company.Illumio;

public class Main {
    public static void main(String[] args) {
        Firewall fw = new Firewall("./rules.csv");

        System.out.println(fw.accept_packet("inbound","tcp",10, "192.168.0.1"));
        System.out.println(fw.accept_packet("outbound","tcp",15000, "192.168.2.2"));//rule 2
        System.out.println(fw.accept_packet("inbound","udp",5, "0.0.0.1"));
        System.out.println(fw.accept_packet("outbound","udp",9, "15.168.0.1"));
        System.out.println(fw.accept_packet("inbound","udp",53, "192.168.1.50"));//rule 3
        System.out.println(fw.accept_packet("inbound","tcp",1000, "52.12.48.92"));

    }
}
