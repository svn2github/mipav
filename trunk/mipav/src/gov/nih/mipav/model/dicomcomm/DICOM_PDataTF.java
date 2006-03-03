package gov.nih.mipav.model.dicomcomm;



public class DICOM_PDataTF extends DICOM_PDUType {

    private DICOM_CommsLink vrLinkedBuffer = new DICOM_CommsLink();

    private int     PDVLength;                  //Presentation Data Value
    public byte    PDVPresContID;
    private byte    PDVMsgHeader;
    private boolean readMessageStatus = false;
    private int     outBlockSize;

    public DICOM_PDataTF() {
        itemType = PDUTYPE_PDataTF;
        setOutgoingBlockSize(0);
    }

    public void writeBody  (DICOM_Comms connection) throws DICOM_Exception {
        // Filler because it extends DICOM_PDUType which is an abstract class
    }

    /**
     *
     * @return DICOM_CommsLink
     */
    public DICOM_CommsLink getVRLinkedBuffer() { return vrLinkedBuffer; }

    /**
    *   Returns true if the reading is complete else false
    *   @return true when reading of the connection is complete
    */
    public boolean isReadComplete() {       return( readMessageStatus ); }

    /**
    *   Sets the outgoing block size based the the Max. Sub. Length
    *   @param outMaxSubLength the maximum sublength
    */
    public void setOutgoingBlockSize( int outMaxSubLength ) {
        if( outMaxSubLength == 0 ) {   outMaxSubLength = DICOM_Constants.MAXSUBLENGTH; }

        outBlockSize = outMaxSubLength - 6;  // ( 4 + 1 + 1 )
        outBlockSize = ( outBlockSize - 1 ) & ~0x01;

        if( outBlockSize < 1024 ) { outBlockSize = 1024; }
    }

    /**
    *   Reads the connection
    *   @param connection the DICOM buffer
    */
    public void readBody( DICOM_Comms connection ) throws DICOM_Exception {
        int count = length;

        readMessageStatus = false;
        while ( count > 0 ) {
            PDVLength     = connection.readInt32();
            PDVPresContID = connection.readByte();
            PDVMsgHeader  = connection.readByte();

            if( PDVLength > count ) {
                throw new DICOM_Exception("PDataTF.readBody: PDV length exceeds pDataTF length" );
            }

            vrLinkedBuffer.readFill( connection, PDVLength - 2); //minus  1 byte = PDVPresContID and 1 byte = PDVMsgHeader

            count  -= PDVLength + 4; // 4 is the PDVLength
            length -= PDVLength + 4;

            if( (PDVMsgHeader & 0x02) == 0x02 ) {
                count = 0; // break out of while loop
            }
        }

        if( (PDVMsgHeader & 0x02) == 0x02 ) { readMessageStatus = true; }
    }

    /**
    *   Writes P-DATA-TF Messages.
    *   @param PDU
    *   @param presentationContextID
    *   @param msgHeader
    */
    public void write( DICOM_PDUService PDU, byte presentationContextID, byte msgHeader ) throws DICOM_Exception {
        PDVPresContID  = presentationContextID;
        PDVMsgHeader   = msgHeader;

        int size  = 0;
        int blockSize = outBlockSize;
        int totalSize = vrLinkedBuffer.getOutgoingSize();

        while( size < totalSize ) {
            if((totalSize - size) < blockSize)   blockSize = totalSize - size;

            if((blockSize + size) == totalSize)  PDVMsgHeader |=  0x02;
            else                                 PDVMsgHeader &= ~0x02;

            PDVLength = blockSize + 2;
            PDU.writeByte(  itemType       );
            PDU.writeByte(  reserved1      );
            PDU.writeInt32( PDVLength + 4  );

            PDU.writeInt32( PDVLength      );
            PDU.writeByte(  PDVPresContID  );
            PDU.writeByte(  PDVMsgHeader   );


            if( blockSize > 0 ) {
                vrLinkedBuffer.flush( PDU, blockSize);
            }

            size += blockSize;
            //System.out.println("Dicom_PData.TF.write - size = " + size + " total size = " + totalSize);
        }
    }


}
