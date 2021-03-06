package simpledb;

import java.util.*;
import java.io.*;

/**
 * Each instance of HeapPage stores data for one page of HeapFiles and
 * implements the Page interface that is used by BufferPool.
 *
 * @see HeapFile
 * @see BufferPool
 */
public class HeapPage implements Page {

    final HeapPageId pid;
    final TupleDesc td;
    final byte header[];
    final Tuple tuples[];
    final int numSlots;
    
    private boolean isdirty;
    private TransactionId madedirty;
    
    private boolean flushedToLog;
    
    byte[] oldData;
    private final Byte oldDataLock = new Byte((byte) 0);

    /**
     * Create a HeapPage from a set of bytes of data read from disk.
     * The format of a HeapPage is a set of header bytes indicating
     * the slots of the page that are in use, some number of tuple slots.
     * Specifically, the number of tuples is equal to: <p>
     * floor((BufferPool.getPageSize()*8) / (tuple size * 8 + 1))
     * <p> where tuple size is the size of tuples in this
     * database table, which can be determined via {@link Catalog#getTupleDesc}.
     * The number of 8-bit header words is equal to:
     * <p/>
     * ceiling(no. tuple slots / 8)
     * <p/>
     *
     * @see Database#getCatalog
     * @see Catalog#getTupleDesc
     * @see BufferPool#getPageSize()
     */
    public HeapPage(HeapPageId id, byte[] data) throws IOException {
        this.pid = id;
        this.td = Database.getCatalog().getTupleDesc(id.getTableId());
        this.numSlots = getNumTuples();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

        // allocate and read the header slots of this page
        header = new byte[getHeaderSize()];
        for (int i = 0; i < header.length; i++)
            header[i] = dis.readByte();

        tuples = new Tuple[numSlots];
        try {
            // allocate and read the actual records of this page
            for (int i = 0; i < tuples.length; i++)
                tuples[i] = readNextTuple(dis, i);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
        dis.close();

        setBeforeImage();
    }

    /**
     * Retrieve the number of tuples on this page.
     *
     * @return the number of tuples on this page
     */
    private int getNumTuples() {
    	int totalbits = (BufferPool.getPageSize() * 8);
    	long tupleSizeAndHeader = td.getSize()*8 + 1 ;
    	int numTuples = (int)Math.floor(totalbits / tupleSizeAndHeader);
        return numTuples;

    }

    /**
     * Computes the number of bytes in the header of a page in a HeapFile with each tuple occupying tupleSize bytes
     *
     * @return the number of bytes in the header of a page in a HeapFile with each tuple occupying tupleSize bytes
     */
    private int getHeaderSize() {
    	int headerSizeinBytes = (int)Math.ceil(this.numSlots / 8.0);
        return headerSizeinBytes;
    }

    /**
     * Return a view of this page before it was modified
     * -- used by recovery
     */
    public HeapPage getBeforeImage() {
        try {
            byte[] oldDataRef = null;
            synchronized (oldDataLock) {
                oldDataRef = oldData;
            }
            return new HeapPage(pid, oldDataRef);
        } catch (IOException e) {
            e.printStackTrace();
            //should never happen -- we parsed it OK before!
            System.exit(1);
        }
        return null;
    }

    public void setBeforeImage() {
        synchronized (oldDataLock) {
            oldData = getPageData().clone();
        }
    }

    /**
     * @return the PageId associated with this page.
     */
    public HeapPageId getId() {
        return this.pid;
    }

    /**
     * Suck up tuples from the source file.
     */
    private Tuple readNextTuple(DataInputStream dis, int slotId) throws NoSuchElementException {
        // if associated bit is not set, read forward to the next tuple, and
        // return null.
        if (!isSlotUsed(slotId)) {
            for (int i = 0; i < td.getSize(); i++) {
                try {
                    dis.readByte();
                } catch (IOException e) {
                    throw new NoSuchElementException("error reading empty tuple");
                }
            }
            return null;
        }

        // read fields in the tuple
        Tuple t = new Tuple(td);
        RecordId rid = new RecordId(pid, slotId);
        t.setRecordId(rid);
        try {
            for (int j = 0; j < td.numFields(); j++) {
                Field f = td.getFieldType(j).parse(dis);
                t.setField(j, f);
            }
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            throw new NoSuchElementException("parsing error!");
        }

        return t;
    }

    /**
     * Generates a byte array representing the contents of this page.
     * Used to serialize this page to disk.
     * <p/>
     * The invariant here is that it should be possible to pass the byte
     * array generated by getPageData to the HeapPage constructor and
     * have it produce an identical HeapPage object.
     *
     * @return A byte array correspond to the bytes of this page.
     * @see #HeapPage
     */
    public byte[] getPageData() {
        int len = BufferPool.getPageSize();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
        DataOutputStream dos = new DataOutputStream(baos);

        // create the header of the page
        for (int i = 0; i < header.length; i++) {
            try {
                dos.writeByte(header[i]);
            } catch (IOException e) {
                // this really shouldn't happen
                e.printStackTrace();
            }
        }

        // create the tuples
        for (int i = 0; i < tuples.length; i++) {

            // empty slot
            if (!isSlotUsed(i)) {
                for (int j = 0; j < td.getSize(); j++) {
                    try {
                        dos.writeByte(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                continue;
            }

            // non-empty slot
            for (int j = 0; j < td.numFields(); j++) {
                Field f = tuples[i].getField(j);
                try {
                    f.serialize(dos);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // padding
        int zerolen = BufferPool.getPageSize() - (header.length + td.getSize() * tuples.length); //- numSlots * td.getSize();
        byte[] zeroes = new byte[zerolen];
        try {
            dos.write(zeroes, 0, zerolen);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    /**
     * Static method to generate a byte array corresponding to an empty
     * HeapPage.
     * Used to add new, empty pages to the file. Passing the results of
     * this method to the HeapPage constructor will create a HeapPage with
     * no valid tuples in it.
     *
     * @return The returned ByteArray.
     */
    public static byte[] createEmptyPageData() {
        int len = BufferPool.getPageSize();
        return new byte[len]; //all 0
    }

    /**
     * Delete the specified tuple from the page;  the tuple should be updated to reflect
     * that it is no longer stored on any page.
     *
     * @param t The tuple to delete
     * @throws DbException if this tuple is not on this page, or tuple slot is
     *                     already empty.
     */
    public void deleteTuple(Tuple t) throws DbException {
        RecordId t_recordId = t.getRecordId();
        if (t_recordId.getPageId() != this.getId()) {
        	throw new DbException("This tuple is not on this page.");
        }
        if (isSlotUsed(t_recordId.tupleno()) == false) {
        	throw new DbException("This slot is not being used.");
        }
        int i = t_recordId.tupleno();
        this.markSlotUsed(i, false);
        tuples[i] = null;
        RecordId newrecordid = new RecordId(null, 0);
        t.setRecordId(newrecordid);
    }

    /**
     * Adds the specified tuple to the page;  the tuple should be updated to reflect
     * that it is now stored on this page.
     *
     * @param t The tuple to add.
     * @throws DbException if the page is full (no empty slots) or tupledesc
     *                     is mismatch.
     */
    public void insertTuple(Tuple t) throws DbException {
        if (this.getNumEmptySlots() == 0){
        	throw new DbException("This page is full.");
        }
        if (this.td.equals(t.getTupleDesc()) == false) {
        	throw new DbException("The tuple descs don't match.");
        } 
        int i = 0;
        for (int j = 0; j < this.getNumTuples(); j++){
        	if (!(isSlotUsed(j))) {
        		i = j;
        		break;
        	}
        }
        RecordId new_recordId = new RecordId(this.getId(), i);
        t.setRecordId(new_recordId);
        markSlotUsed(i, true);
        tuples[i] = t;
        
    }

    /**
     * Marks this page as dirty/not dirty and record that transaction
     * that did the dirtying
     */
    public void markDirty(boolean dirty, TransactionId tid) {
        this.isdirty = dirty;
        this.madedirty = tid;
    }

    /**
     * Returns the tid of the transaction that last dirtied this page, or null if the page is not dirty
     */
    public TransactionId isDirty() {
        if (this.isdirty) {
        	return this.madedirty;
        }
        return null;
    }
    
    /**
     * 
     */
    public void markFlushedToLog(boolean flushed) {
    	this.flushedToLog = flushed;
    }
    
    public boolean isFlushedToLog() {
    	return this.flushedToLog;
    }
    
    /**
     * Counts the number of 0s in the bit representation of the int value. 
     */
    private int bitCount(int value){
    	int count = 0;
    	int tocompare = 1;
    	while (tocompare < Math.pow(2, 8)){
    		if ((value & tocompare) == 0)
    			count++;
    		tocompare <<= 1;
    	}
    	return count;
    }

    /**
     * Returns the number of empty slots on this page.
     */
    public int getNumEmptySlots() {
        int count = 0;
        for (int i = 0; i < header.length - 1; i++){
        	Byte thisByte = header[i];	
        	count += bitCount(thisByte);
        } 
        // handling the last byte separately 
        // because the higher order bits may not be used.
        Byte lastbyte = header[header.length - 1];
        int lastbyteint = lastbyte.intValue();
        int slotsusedinlastbyte = this.getNumTuples() % 8;
        lastbyteint <<= slotsusedinlastbyte;
        lastbyteint >>= slotsusedinlastbyte;
        count += bitCount(lastbyteint);
        return count;
    }

    /**
     * Returns true if associated slot on this page is filled.
     */
    public boolean isSlotUsed(int i) {
        int slotByteNo = (i / 8);
        if (slotByteNo > getHeaderSize() - 1){
        	return false;
        }
        Byte headerByte = header[slotByteNo];
        int bitInByte = i % 8; 
        int mask = 1;
        mask <<= bitInByte;
        if ((mask&headerByte) == 0) {
        	return false;
        }
        return true;
    }

    /**
     * Abstraction to fill or clear a slot on this page.
     */
    private void markSlotUsed(int i, boolean value) {
    	int slotByteNo = (i / 8);
        Byte headerByte = header[slotByteNo];
        int bitInByte = i % 8;
        int mask;
        if (value == false) {
        	mask = 1;
        	mask <<= bitInByte;
        	mask = ~(mask);
        	this.header[slotByteNo] = (byte) (mask&headerByte); 
        }
        else {
        	mask = 1;
        	mask <<= bitInByte;
        	this.header[slotByteNo] = (byte) (mask | headerByte);
        }
            	
    }

    /**
     * @return an iterator over all tuples on this page (calling remove on this iterator throws an UnsupportedOperationException)
     * (note that this iterator shouldn't return tuples in empty slots!)
     */
    public Iterator<Tuple> iterator() {
        Iterator<Tuple> it = new myIterator(this, tuples);
        return it;
    }
    
    class myIterator implements Iterator<Tuple> {

        private int currIdx;
        private Tuple next = null;
        private HeapPage heappage = null;
        private Tuple tuples[] = null;
        
        public myIterator(HeapPage heappage, Tuple[] tuples) {
            this.tuples = tuples;
            this.heappage = heappage;
            currIdx = 0;
            next = null;
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                fetchNext();
            }
            return next != null;
        }

        /**
         * Helper method: looks ahead to find the next element that
         * matches the criteria.
         */
        private void fetchNext() {
            // keep going as long as the current item does not match
            // the criteria
            while (currIdx < tuples.length && heappage.isSlotUsed(currIdx)==false) {
                currIdx++;
            }
            // did we find a match or reach the end of the data?
            if (currIdx < tuples.length) {
                next = tuples[currIdx];
                currIdx++;           
            }
        }

        @Override
        public Tuple next() {
            if (!hasNext()) {
                throw new NoSuchElementException("");
            }
            Tuple returnValue = next;
            next = null;                
            return returnValue;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("my data can't be modified...  or maybe I'm just being lazy.");
        }

    }
    
}




