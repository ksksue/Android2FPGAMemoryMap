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
 * 
 * Address/Data packet
 * Figure 18–3. Bits to Avalon-MM Transaction
 */
public class AvalonSTParser {
    static final byte ESC   = 0x7d; // Escape
    static final byte SOP   = 0x7a; // Start of Packet
    static final byte EOP   = 0x7b; // End of Packet
    static final byte CNI   = 0x7c; // Channel Number Indicator

    static final byte TRAN_WRITE            = 0x00; // Write, non-incrementing address
    static final byte TRAN_WRITE_INC_ADDR   = 0x04; // Write, incrementing address
    static final byte TRAN_READ             = 0x10; // Read, non-incrementing address
    static final byte TRAN_READ_INC_ADDR    = 0x14; // Read, incrementing address
    static final byte TRAN_NO_TRAN          = 0x7F; // No transaction

    public AvalonSTParser() {
    }

    /**
     * Convert address and data to an Avalon-ST Write Non-incrementing Address packet
     * @param address Avalon-ST 4byte address
     * @param data Avalon-ST 4byte data
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createWritePacket(String address, String data) throws Exception{
        return createPacket(TRAN_WRITE, address, data, 0);
    }

    /**
     * Convert address and data to an Avalon-ST Write Incrementing Address packet
     * @param address Avalon-ST 4byte address
     * @param data Avalon-ST 4byte data
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createWriteIncAddressPacket(String address, String data) throws Exception{
        return createPacket(TRAN_WRITE_INC_ADDR, address, data, 0);
    }

    /**
     * Convert address and data to an Avalon-ST Read Non-incrementing Address packet
     * @param address Avalon-ST 4byte address
     * @param byteSize read data byte size
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createReadPacket(String address, int byteSize) throws Exception{
        return createPacket(TRAN_READ, address, "", byteSize);
    }

    /**
     * Convert address and data to an Avalon-ST Read Incrementing Address packet
     * @param address Avalon-ST 4byte address
     * @param byteSize read data byte size
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createReadIncAddressPacket(String address, int byteSize) throws Exception{
        return createPacket(TRAN_READ_INC_ADDR, address, "", byteSize);
    }



    /**
     * Convert address and data to an Avalon-ST Write Non-incrementing Address packet
     * @param address Avalon-ST address
     * @param data Avalon-ST data
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createWritePacket(byte[] address, byte[] data) throws Exception{
        return createPacket(TRAN_WRITE, address, data, data.length);
    }

    /**
     * Convert address and data to an Avalon-ST Write Incrementing Address packet
     * @param address Avalon-ST address
     * @param data Avalon-ST data
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createWriteIncAddressPacket(byte[] address, byte[] data) throws Exception{
        return createPacket(TRAN_WRITE_INC_ADDR, address, data, data.length);
    }

    /**
     * Convert address and data to an Avalon-ST Write Non-incrementing Address packet
     * @param address Avalon-ST address
     * @param data Avalon-ST data
     * @param byteSize write data byte size
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createWritePacket(byte[] address, byte[] data, int byteSize) throws Exception{
        return createPacket(TRAN_WRITE, address, data, byteSize);
    }

    /**
     * Convert address and data to an Avalon-ST Write Incrementing Address packet
     * @param address Avalon-ST address
     * @param data Avalon-ST data
     * @param byteSize write data byte size
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createWriteIncAddressPacket(byte[] address, byte[] data, int byteSize) throws Exception{
        return createPacket(TRAN_WRITE_INC_ADDR, address, data, byteSize);
    }

    /**
     * Convert address and data to an Avalon-ST Read Non-incrementing Address packet
     * @param address Avalon-ST address
     * @param data Avalon-ST data
     * @param byteSize read data byte size
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createReadPacket(byte[] address, int byteSize) throws Exception{
        byte[] data = new byte[1];
        data[0] = 0;
        return createPacket(TRAN_READ, address, data, byteSize);
    }

    /**
     * Convert address and data to an Avalon-ST Read Incrementing Address packet
     * @param address Avalon-ST address
     * @param data Avalon-ST data
     * @param byteSize read data byte size
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createReadIncAddressPacket(byte[] address, int byteSize) throws Exception{
        byte[] data = new byte[1];
        data[0] = 0;
        return createPacket(TRAN_READ_INC_ADDR, address, data, byteSize);
    }

    /**
     * Convert address and data to an Avalon-ST packet
     * @param transaction Transaction code
     * @param address Avalon-ST address
     * @param data Avalon-ST data
     * @param byteSize read/write data byte size
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createPacket(byte transaction, String address, String data, int byteSize) throws Exception{

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
        if(transaction == TRAN_WRITE || transaction == TRAN_WRITE_INC_ADDR) {
            if(data.length()==0) {
                byte[] noData = new byte[1];
                noData[0] = 0;
                return noData;
            }

            int byteLength = (data.length()+1)/2;
            byte[] bData = new byte[byteLength];
            int iData;
            iData = Integer.decode("0x"+data).intValue();

            for(int i=0; i<byteLength; i++) {
                bData[i] = (byte)((byte)0xff & (iData >> 8*(byteLength-1-i)));
            }
            return createPacket(transaction, bAddress, bData, bData.length);
        } else {
            byte[] bData = new byte[1];
            bData[0] = 0;
            return createPacket(transaction, bAddress, bData, byteSize);
        }
        ///////////////////////////////////////////////////

    }

    /**
     * Convert address and data to an Avalon-ST packet
     * @param transaction Transaction code
     * @param packet output created packet
     * @param address 4 byte Avalon-ST address
     * @param data 1-n byte Avalon-ST data
     * @param byteSize read/write data byte size
     * @return Avalon-ST packet
     * @throws Exception
     */
    public byte[] createPacket(byte transaction, byte[] address, byte[] data, int byteSize) throws Exception{
        ArrayList<Byte> packetArray= new ArrayList<Byte>();
        createPacket(packetArray, transaction, address, data, byteSize);

        byte[] packet = new byte[packetArray.size()];
        for(int i=0; i<packetArray.size(); i++) {
            packet[i] = packetArray.get(i).byteValue();
        }

        return packet;
    }

    /**
     * Convert address and data to an Avalon-ST packet
     * @param packet output created packet
     * @param transaction Transaction code
     * @param address 4 byte avalon-st address
     * @param data 1-n byte avalon-st data
     * @param byteSize read/write data byte size
     * @throws Exception
     */
    public void createPacket(ArrayList<Byte> packet,byte transaction, byte[] address, byte[] data, int byteSize) throws Exception{

        ////////////////////////////////
        // Create Packet
        ////////////////////////////////
        packet.clear();
        packet.add((byte) transaction);                         // Transaction code
        packet.add((byte) 0x00);                                // Reserved
        packet.add((byte) ((byte)0xff & (byteSize >> 8)));     // Size
        packet.add((byte) ((byte)0xff &  byteSize));           // Size

        for(byte addrByte : address) {
            packet.add(addrByte);                               // Address
        }

        if(transaction == TRAN_WRITE || transaction == TRAN_WRITE_INC_ADDR) {
            for(int i=0; i<byteSize; i++) {
                packet.add(data[i]);                               // Data
            }
        } else {
            packet.add((byte)0);                               // Data
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
