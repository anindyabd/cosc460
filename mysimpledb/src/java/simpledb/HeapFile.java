package simpledb;

import java.io.*;

import java.nio.ByteBuffer;
import java.util.*;

import simpledb.HeapPage.myIterator;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @author Sam Madden
 * @see simpledb.HeapPage#HeapPage
 */
public class HeapFile implements DbFile {

    
	private File file;
	private TupleDesc td;
	/**
     * Constructs a heap file backed by the specified file.
     *
     * @param f the file that stores the on-disk backing store for this heap
     *          file.
     */
    public HeapFile(File f, TupleDesc td) {
        this.file = f;
        this.td = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        return this.file; 
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        return this.file.getAbsoluteFile().hashCode(); 
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        return this.td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
    	InputStream input = null;
    	try {
    		input = new BufferedInputStream(new FileInputStream(file));
    		int pgNo = pid.pageNumber();
    		long offset = pgNo * BufferPool.PAGE_SIZE;
    		byte bytearray[] = new byte[BufferPool.PAGE_SIZE];
    		try {
    			input.skip(offset);
    			input.read(bytearray, 0, BufferPool.PAGE_SIZE);
    			HeapPage heappage = new HeapPage((HeapPageId)pid, bytearray);
    			
    			return heappage;
    		}
    		catch (IOException e){
    			return null;
    		}
    		
    	}
    	catch (FileNotFoundException e) {
    		System.out.println("File Not Found Exception");
    		return null;
    	}
    	finally {
    		try {
    			input.close();
    		}
    		catch (IOException e){
    		}
    	}
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        return (int) Math.ceil(this.file.length()/BufferPool.PAGE_SIZE);
    }

    // see DbFile.java for javadocs
   public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        myHeapFileIterator it = new myHeapFileIterator(tid, this.getId(), this.numPages());
        return it;
    }
    
    class myHeapFileIterator implements DbFileIterator {
    	
    	private int pgNo;
    	private TransactionId tid;
    	private int heapfileid;
    	private int numPages;
    	private HeapPage currpage;
    	private myIterator<Tuple> heappageiterator;
    	
    	public myHeapFileIterator(TransactionId tid, int heapfileid, int numPages){
    		this.tid = tid;
    		this.heapfileid = heapfileid;
    		this.numPages = numPages;
    		pgNo = 0;
    	}
    	
    	public void open() throws TransactionAbortedException, DbException{
    		Permissions perm = Permissions.READ_ONLY;
    		HeapPageId heappageid = new HeapPageId(heapfileid, pgNo);
    		BufferPool bufferpool = Database.getBufferPool();
    		HeapPage page = (HeapPage)bufferpool.getPage(tid, heappageid, perm);
    		currpage = page;
    		heappageiterator = (myIterator<Tuple>) currpage.iterator();
    	}
    	
    	public boolean hasNext(){
    		if (currpage == null) return false;
    		if (heappageiterator.hasNext()){
    			return true;
    		}
    		else if (pgNo < this.numPages - 1) {
    			return true;
    		}
    		else return false;
    	}
    	
    	
    	public Tuple next() throws TransactionAbortedException, DbException{
    		if (!hasNext()){
    			throw new NoSuchElementException("");
    		}
    		if (heappageiterator.hasNext()){
    			return heappageiterator.next();
    		}
    		else {
    			this.pgNo++;
    			Permissions perm = Permissions.READ_ONLY;
    			HeapPageId heappageid = new HeapPageId(heapfileid, pgNo);
        		BufferPool bufferpool = Database.getBufferPool();
        		HeapPage page = (HeapPage)bufferpool.getPage(tid, heappageid, perm);
        		currpage = page;
        		heappageiterator = (myIterator<Tuple>) currpage.iterator();
        		return heappageiterator.next();
    		}
    	}
    	
    	public void rewind(){
    		pgNo = 0;
    		currpage = null;
    	}
    	
    	public void close(){
    		currpage = null;
    	}
    }

}

