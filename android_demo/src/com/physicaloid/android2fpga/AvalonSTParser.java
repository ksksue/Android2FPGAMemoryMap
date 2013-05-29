package com.physicaloid.android2fpga;

import java.util.ArrayList;

/*
 * reference to http://www.altera.com/literature/ug/ug_embedded_ip.pdf
 * 20. Avalon-ST Bytes to Packets and Packets to Bytes Converter Cores
 * 
 * Operation
 * "
 * Escape (0x7d)—The core drops the byte. The next byte is XORed with 0x20.
 *
 * Start of packet (0x7a)—The core drops the byte and marks the next payload byte as
 *  the start of a packet by asserting the startofpacket signal on the Avalon-ST
 *  source interface.
 *
 * End of packet (0x7b)—The core drops the byte and marks the following byte as the
 *  end of a packet by asserting the endofpacket signal on the Avalon-ST source
 *  interface. For single beat packets, both the startofpacket and endofpacket signals
 *  are asserted in the same clock cycle.
 *
 * Channel number indicator (0x7c)—The core drops the byte and takes the next nonspecial
 *  character as the channel number.
 * "
 *
 * Packet Formats
 * "
 * Table 21–2. Packet Formats
 * Byte  | Field            | Description
 * Transaction Packet Format
 * 0     | Transaction code | Type of transaction. Refer to Table 21–3.
 * 1     | Reserved         | Reserved for future use.
 * [3:2] | Size             | Transaction size in bytes. For write transactions,
 *                          | the size indicates the size of the data field.
 *                          | For read transactions, the size indicates the total number of bytes to read.
 * [7:4] | Address          | 32-bit address for the transaction.
 * [n:8] | Data             | Transaction data; data to be written for write transactions.
 * Response Packet Format
 * 0     | Transaction code | The transaction code with the most significant bit inversed.
 * 1     | Reserved         | Reserved for future use.
 * [3:2] | Size             | Total number of bytes written successfully.
 * "
 */
public class AvalonSTParser {
    static final byte ESC   = 0x7d; // Escape
    static final byte SOP   = 0x7a; // Start of Packet
    static final byte EOP   = 0x7b; // End of Packet
    static final byte CNI   = 0x7c; // Channel Number Indicator

    byte[] command;
    byte[] address;
    byte[] data;

    public AvalonSTParser() {
    }

    /**
     * Convert address and data to an Avalon-ST packet
     * @param address Avalon-ST address
     * @param data Avalon-ST data
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createPacket(String address, String data) throws Exception{

        ///////////////////////////////////////////////////
        // Converts int Address to byte array Address
        ///////////////////////////////////////////////////
        byte[] bAddress = new byte[4];
        int iAddress;
        iAddress = Integer.decode("0x"+address).intValue();

        bAddress[0] = (byte)((byte)0xff & (iAddress >> 8*3));
        bAddress[1] = (byte)((byte)0xff & (iAddress >> 8*2));
        bAddress[2] = (byte)((byte)0xff & (iAddress >> 8*1));
        bAddress[3] = (byte)((byte)0xff & (iAddress));
        ///////////////////////////////////////////////////

        ///////////////////////////////////////////////////
        // Converts int Data to byte Data
        ///////////////////////////////////////////////////
        byte[] bData = new byte[4];
        int iData;
        iData = Integer.decode("0x"+data).intValue();

        bData[0] = (byte)((byte)0xff & (iData >> 8*3));
        bData[1] = (byte)((byte)0xff & (iData >> 8*2));
        bData[2] = (byte)((byte)0xff & (iData >> 8*1));
        bData[3] = (byte)((byte)0xff & (iData));
        ///////////////////////////////////////////////////

        return createPacket(bAddress, bData);
    }

    /**
     * Convert address and data to an Avalon-ST packet
     * @param packet output created packet
     * @param address 4 byte Avalon-ST address
     * @param data 1-n byte Avalon-ST data
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createPacket(byte[] address, byte[] data) throws Exception{
        ArrayList<Byte> packetArray= new ArrayList<Byte>();
        createPacket(packetArray, address, data);

        byte[] packet = new byte[packetArray.size()];
        for(int i=0; i<packetArray.size(); i++) {
            packet[i] = packetArray.get(i).byteValue();
        }

        return packet;
    }

    /**
     * Convert address and data to an Avalon-ST packet
     * @param packet output created packet
     * @param address 4 byte avalon-st address
     * @param data 1-n byte avalon-st data
     * @throws Exception
     */
    public void createPacket(ArrayList<Byte> packet, byte[] address, byte[] data) throws Exception{
        int data_size = data.length;

        packet.clear();
        packet.add((byte) 0x00);
        packet.add((byte) 0x00);
        packet.add((byte) ((byte)0xff & (data_size >> 8)));
        packet.add((byte) ((byte)0xff &  data_size));

        for(byte addrByte : address) {
            packet.add(addrByte);
        }

        for(byte dataByte : data) {
            packet.add(dataByte);
        }

        ////////////////////////////////
        // Search Escape Byte
        ////////////////////////////////
        int index = 0;
        int len = packet.size();
        byte b = 0;
        for(int i=0; i<len; i++) {
            b=packet.get(index);
            if(     b == ESC ||
                    b == SOP ||
                    b == EOP ||
                    b == CNI) {
                packet.add(index, ESC);
                index++;
                packet.set(index, (byte)(packet.get(index) ^ (byte)0x20)); // XORed with 0x20
                index++;
            } else {
                index++;
            }
        }

        ////////////////////////////////
        // Insert SOP, CNI and EOP
        ////////////////////////////////
        packet.add(0, SOP);
        index++;
        packet.add(1, CNI);
        index++;
        packet.add(2, (byte)0x00);  // always channel 0
        index++;
        packet.add(index-1, EOP);   // insert EOP at last-1
    }
}
