import jpcap.*;
import jpcap.packet.*;

import java.io.IOException;
import java.util.ArrayList;
import java.net.InetAddress;
import java.util.Scanner;

public class Main {
    //����Ĭ�����ץ����
    private static final int max = 4096;
    //���巢�͵ı��ĵ�Դ��ַ
    private static final String src = "10.132.29.197";
    //���巢�͵ı��ĵ�Ŀ�ĵ�ַ
    private static final String dst = "192.168.253.134";

    //��ʾ���������豸��Ϣ
    private static void showDeviceList(NetworkInterface[] devices) {
        System.out.println("�������������������£�");
        for (int i = 0; i < devices.length; i++) {
            //��������������
            System.out.println("Adapter " + (i + 1) + "��" + devices[i].description);
            //MAC��ַ
            System.out.print("    MAC address: ");
            for (byte b : devices[i].mac_address) {
                System.out.print(Integer.toHexString(b & 0xff) + ":");
            }
            System.out.println();
            //IP��ַ
            for (NetworkInterfaceAddress a : devices[i].addresses) {
                System.out.println("    IPv6/IPv4 address: " + a.address);
            }
            System.out.println();
        }
    }

    //����ӿڼ���
    private static JpcapCaptor openDevice(NetworkInterface[] devices, int choice) throws java.io.IOException{
        JpcapCaptor captor = null;
        try{
            captor = JpcapCaptor.openDevice(devices[choice], 65535, false, 3000);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("������ӿ�ʧ�ܣ�");

        }
        return captor;
    }

    //���ݰ������߳�
    private static class AThread implements Runnable{
        Thread thread;
        JpcapCaptor captor;
        Packet[] packet;
        //�߳��жϱ�־
        volatile boolean cancel;

        AThread(JpcapCaptor captor) throws IOException{
            this.captor = captor;
            this.packet = new Packet[max];
            this.cancel = false;
            thread = new Thread(this);
        }

        @Override
        public void run() {
            packet = new Packet[max];
            for(int i = 0; i < max && cancel == false; i++){
                packet[i] = captor.getPacket();
            }
        }

        public void cancel(){
            cancel = true;
        }

        public Packet[] getPacket(){
            return packet;
        }

    }

    private static void showPacket(Packet[] packet){
        for(int i = 0; packet[i] != null && i < max; i++){
            System.out.println("Packet " + (i+1) + " : " + packet[i]);
        }
    }

    private static Packet[] readPacket(JpcapCaptor captor, String filename){
        Packet[] packet = new Packet[max];
        try {
            captor = JpcapCaptor.openFile(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(int i = 0;;i++){
            packet[i] = captor.getPacket();
            if(packet[i] == null)
                break;
        }
        return packet;
    }

    private static void savePacket(JpcapCaptor capter, Packet[] packet) {
        JpcapWriter writer = null;
        try {
            writer = JpcapWriter.openDumpFile(capter, "./savePacket");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(int i = 0 ; packet[i] != null; i++){
            writer.writePacket(packet[i]);
        }

        writer.close();
    }

    private static void analyzePacket(Packet[] packet){

        ArrayList<UDPPacket> udpPacketArray = new ArrayList<UDPPacket>();
        ArrayList<ICMPPacket> icmpPacketArray = new ArrayList<ICMPPacket>();
        ArrayList<ARPPacket> arpPacketArray = new ArrayList<ARPPacket>();
        ArrayList<TCPPacket> tcpPacketArray = new ArrayList<TCPPacket>();
        ArrayList<Packet> unknownPacketArray = new ArrayList<Packet>();

        int count, count1, count2, count3, count4, count5;
        count = count1 = count2 = count3 = count4 = count5 = 0;

        for(int i = 0; packet[i] != null && i < max; i++) {
            count++;

            if (packet[i] instanceof UDPPacket) {
                UDPPacket udp = (UDPPacket) packet[i];
                udpPacketArray.add(udp);
                count1++;
            }else if(packet[i] instanceof ICMPPacket){
                ICMPPacket icmp = (ICMPPacket) packet[i];
                icmpPacketArray.add(icmp);
                count2++;
            }else if(packet[i] instanceof ARPPacket){
                ARPPacket arp = (ARPPacket) packet[i];
                arpPacketArray.add(arp);
                count3++;
            }else if(packet[i] instanceof TCPPacket){
                TCPPacket tcp = (TCPPacket) packet[i];
                tcpPacketArray.add(tcp);
                count4++;
            }else{
                unknownPacketArray.add(packet[i]);
                count5++;
            }
        }

        System.out.println();
        System.out.println("�������ݰ�����" + count);
        System.out.println("UDP���ݰ�����" + count1);
        System.out.println("ICMP���ݰ�����" + count2);
        System.out.println("ARP���ݰ�����" + count3);
        System.out.println("TCP���ݰ�����" + count4);
        System.out.println("�������ݰ�����" + count5);

    }

    private static void showPacketDetail(Packet[] packet){
        for(int i = 0; packet[i] != null && i < max; i++) {
            if(packet[i] instanceof UDPPacket){
                UDPPacket udp = (UDPPacket) packet[i];
                String data = new String(udp.data);
                System.out.println("Packet " + (i+1) + " : UDP" );
                System.out.println("    source ip : " + udp.src_ip.toString());
                System.out.println("    destination ip : " + udp.dst_ip.toString());
                System.out.println("    source port : " + String.valueOf(udp.src_port));
                System.out.println("    destination port : " + String.valueOf(udp.dst_port));
                System.out.println("    offset : " + String.valueOf(udp.offset));
                System.out.println("    data : " + data);
            }else if(packet[i] instanceof TCPPacket){
                TCPPacket tcp = (TCPPacket) packet[i];
                String data = new String(tcp.data);
                System.out.println("Packet " + (i+1) + " : TCP" );
                System.out.println("    source ip : " + tcp.src_ip.toString());
                System.out.println("    destination ip : " + tcp.dst_ip.toString());
                System.out.println("    source port : " + String.valueOf(tcp.src_port));
                System.out.println("    destination port : " + String.valueOf(tcp.dst_port));
                System.out.println("    offset : " + String.valueOf(tcp.offset));
                System.out.println("    data : " + data );
            }else if(packet[i] instanceof ARPPacket){
                ARPPacket arp = (ARPPacket) packet[i];
                byte[] b = new byte[4];
                String s1 = "";
                String s2 = "";

                b = arp.target_protoaddr;
                s1 += String.valueOf((b[0] & 0xff) + "." + ( b[1] & 0xff) + "." +
                        (b[2] & 0xff) + "." + (b[3] & 0xff));
                b = arp.sender_protoaddr;
                s2 += String.valueOf((b[0] & 0xff) + "." + ( b[1] & 0xff) + "." +
                        (b[2] & 0xff) + "." + (b[3] & 0xff));

                System.out.println("Packet " + (i+1) + " : ARP" );
                System.out.println("    sender address: " + s2);
                System.out.println("    target address: " + s1);
            }else if(packet[i] instanceof ICMPPacket){
                ICMPPacket icmp = (ICMPPacket) packet[i];

                System.out.println("Packet " + (i+1) + " : ICMP");
                System.out.println("    ICMP packet.");
            }else{
                System.out.println("Packet " + (i+1) + " : " );
                System.out.println("    no information.");
            }

        }
    }

    private static IPPacket generateIpPacket() throws java.io.IOException{

        Scanner scanner = new Scanner(System.in);
        System.out.print("������Ҫ���͵�����: ");
        String data = scanner.next();

        //����ether֡��frame��
        EthernetPacket ether = new EthernetPacket();
        //����֡����ΪIP
        ether.frametype = EthernetPacket.ETHERTYPE_IP;
        //����Դ��Ŀ��MAC��ַ
        ether.src_mac = "30:52:cb:f0:6f:f6".getBytes();
        ether.dst_mac = "00:0c:29:3c:0a:f1".getBytes();

        //����IP����
        IPPacket ipPacket = new IPPacket();
        ipPacket.setIPv4Parameter(0,false,false,false,0,false,false,
                false,0,0,128,230,InetAddress.getByName(src),
                InetAddress.getByName(dst));
        ipPacket.data = (data).getBytes();
        ipPacket.datalink = ether;

        return ipPacket;
    }

    private static TCPPacket generateTcpPacket() throws java.io.IOException{
        Scanner scanner = new Scanner(System.in);
        System.out.print("������Ҫ���͵�����: ");
        String data = scanner.next();

        //����ether֡��frame��
        EthernetPacket ether = new EthernetPacket();
        //����֡����ΪIP
        ether.frametype = EthernetPacket.ETHERTYPE_IP;
        //����Դ��Ŀ��MAC��ַ
        ether.src_mac = "30:52:cb:f0:6f:f6".getBytes();
        ether.dst_mac = "00:0c:29:3c:0a:f1".getBytes();

        //����TCP����
        TCPPacket tcpPacket = new TCPPacket(12, 34, 56, 78, false, false,
                false, false, true, true, true, true, 10, 0);
        //����IPͷ
        tcpPacket.setIPv4Parameter(0,false,false,false,0,false,false,
                false,0,65,128,IPPacket.IPPROTO_TCP,InetAddress.getByName(src),
                InetAddress.getByName(dst));
        //���ñ�������
        tcpPacket.data = (data).getBytes();

        //����������·��
        tcpPacket.datalink = ether;

        return tcpPacket;
    }

    private static ARPPacket generateArpPacket() throws java.io.IOException{

        //����ether֡��frame��
        EthernetPacket ether = new EthernetPacket();
        //����֡����ΪIP
        ether.frametype = EthernetPacket.ETHERTYPE_ARP;
        //����Դ��Ŀ��MAC��ַ
        ether.src_mac = "30:52:cb:f0:6f:f6".getBytes();
        ether.dst_mac = new byte[]{(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255};

        //����ARP����
        ARPPacket arpPacket = new ARPPacket();
        arpPacket.hardtype = ARPPacket.HARDTYPE_ETHER;//Ӳ������
        arpPacket.prototype = ARPPacket.PROTOTYPE_IP;//Э������
        arpPacket.operation = ARPPacket.ARP_REQUEST;//ָ��ΪARP������(��һ��Ϊ�ظ�����)
        arpPacket.hlen = 6;//�����ַ����
        arpPacket.plen = 4;//Э���ַ����
        arpPacket.sender_hardaddr = ether.src_mac;//���Ͷ�Ϊ����mac��ַ
        arpPacket.sender_protoaddr = InetAddress.getByName(src).getAddress();//����IP��ַ
        arpPacket.target_hardaddr = ether.dst_mac; //Ŀ�Ķ�mac��ַΪ�㲥��ַ
        arpPacket.target_protoaddr = InetAddress.getByName(dst).getAddress();//Ŀ��IP��ַ
        arpPacket.datalink = ether;//����arp����������·��

        return arpPacket;
    }

    private static UDPPacket generateUdpPacket() throws java.io.IOException{

        Scanner scanner = new Scanner(System.in);
        System.out.print("������Ҫ���͵�����: ");
        String data = scanner.next();

        //������̫֡��frame��
        EthernetPacket ether = new EthernetPacket();
        //����֡����ΪIP
        ether.frametype = EthernetPacket.ETHERTYPE_IP;
        //����Դ��Ŀ��MAC��ַ
        ether.src_mac = "30:52:cb:f0:6f:f6".getBytes();
        ether.dst_mac = "00:0c:29:3c:0a:f1".getBytes();

        //����UDP����
        UDPPacket udpPacket = new UDPPacket(12, 34);
        udpPacket.src_ip = InetAddress.getByName(src);
        udpPacket.dst_ip = InetAddress.getByName(dst);
        udpPacket.data = data.getBytes();

        //����IPͷ
        udpPacket.setIPv4Parameter(0,false,false,false,0,false,false,
                false,0,65,128,IPPacket.IPPROTO_UDP,InetAddress.getByName(src),
                InetAddress.getByName(dst));
        udpPacket.datalink = ether;

        return udpPacket;
    }

    private static ICMPPacket generateIcmpPacket() throws java.io.IOException{

        //������̫֡��frame��
        EthernetPacket ether = new EthernetPacket();
        //����֡����ΪIP
        ether.frametype = EthernetPacket.ETHERTYPE_IP;
        //����Դ��Ŀ��MAC��ַ
        ether.src_mac = "30:52:cb:f0:6f:f6".getBytes();
        ether.dst_mac = new byte[]{(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255};

        //����ICMP����
        ICMPPacket icmpPacket = new ICMPPacket();
        icmpPacket.type = ICMPPacket.ICMP_ECHO;//���ͻ���������
        icmpPacket.data = "test".getBytes();

        //����IPV4ͷ
        icmpPacket.setIPv4Parameter(0,false,false,false,0,false,false,
                false,0,65,128,IPPacket.IPPROTO_ICMP,InetAddress.getByName(src),
                InetAddress.getByName(dst));

        //������̫֡ͷ��
        icmpPacket.datalink = ether;

        return icmpPacket;

    }

    public static void main(String[] args) throws java.io.IOException{

        //��ȡ�û�����
        Scanner scanner = new Scanner(System.in);

        //������ݰ�
        Packet[] packet = new Packet[max];

        //��ʼ�����ݰ�������߳�
        AThread t = null;

        //��ȡ�����豸����ʾ
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();
        showDeviceList(devices);

        //����ѡ��ļ�ص�����
        System.out.print("����ѡ����������������:");
        int card = scanner.nextInt();
        card = card -1;
        System.out.println();

        //��ѡ�������ӿ�
        JpcapCaptor captor = openDevice(devices, card);

        menu:
        while(true) {
            //���ܲ˵�
            System.out.println("��ѡ��ʹ�õĹ��ܱ�ţ�");
            System.out.println("1. ����ǰ���������ݰ�");
            System.out.println("2. ֹͣ�����������ݰ�");
            System.out.println("3. ���뱾�ص��������ݰ�");
            System.out.println("4. ��ʾ��ǰ��������ݰ�");
            System.out.println("5. ���浱ǰ���������ݰ�");
            System.out.println("6. �������ݰ���Э��ֲ�");
            System.out.println("7. �鿴���ݰ���ϸ��Ϣ");
            System.out.println("8. �������ݰ���Ŀ������");
            System.out.println("9. �˳�");
            System.out.print("���ѡ��");
            //�û�ѡ��
            int choice = scanner.nextInt();

            //����ִ��
            switch (choice){
                case 1: System.out.println("���ڲ������ݰ�...");
                        t = new AThread(captor);
                        Thread capThread = new Thread(t);
                        capThread.start();
                        break;
                case 2: System.out.println("��ֹͣ�������ݰ�");
                        t.cancel();
                        break;
                case 3: packet = readPacket(captor, "./savePacket");
                        System.out.println("�ѵ��뱾�����ݰ�");
                        break;
                case 4: System.out.println("��ʾ��ǰ��������ݰ����£�");
                        if(t == null){
                            System.out.println("���ݰ�������δ����");
                            break;
                        }
                        packet = t.getPacket();
                        showPacket(packet);
                        break;
                case 5: savePacket(captor, packet);
                        System.out.println("�ѱ������ݰ���Ĭ��λ��");
                        break;
                case 6: System.out.println("���ݰ���Э��ֲ����£�");
                        analyzePacket(packet);
                        break;
                case 7: System.out.println("���ݰ���ϸ��Ϣ���£�");
                        showPacketDetail(packet);
                        break;
                case 8: System.out.print("��ѡ���͵�Э������(IP��TCP��UDP��ICMP��ARP): ");
                        JpcapSender sender = JpcapSender.openDevice(devices[card]);
                        String type = scanner.next().toUpperCase();
                        if(type.equals("IP")){
                            sender.sendPacket(generateIpPacket());
                        }else if(type.equals("TCP")) {
                            sender.sendPacket(generateTcpPacket());
                        }else if(type.equals("UDP")) {
                            sender.sendPacket(generateUdpPacket());
                        }else if(type.equals("ICMP")) {
                            sender.sendPacket(generateIcmpPacket());
                        }else if(type.equals("ARP")) {
                            sender.sendPacket(generateArpPacket());
                        }else {
                            System.out.println("����Э�����ʹ���");
                            break;
                        }
                        sender.close();
                        System.out.println("�ѷ������ݰ���Ŀ���ַ");
                        break;
                case 9: break menu;
            }
            System.out.println();
        }

        //�ر�
        captor.close();

    }

}
