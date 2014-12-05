package simpledb;

import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p/>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 *
 * @Threadsafe, all fields are final
 */
public class BufferPool {
	
	
    /**
     * Bytes per page, including header.
     */
    public static final int PAGE_SIZE = 4096;

    private static int pageSize = PAGE_SIZE;

    /**
     * Default number of pages passed to the constructor. This is used by
     * other classes. BufferPool should use the numPages argument to the
     * constructor instead.
     */
    public static final int DEFAULT_PAGES = 50;

    /* maps PageIDs to pages */
    private HashMap<PageId, Page> pagemap;
    
    /* maximum number of pages in this buffer pool. */
    private int numPages;
    
    /* number of pages currently in the buffer pool. */
    private int currpages; 
    
    /* Maps retrieval time to pages, for LRU policy. */
    private TreeMap<Long, Page> timemap;
    
    private static LockManager lm;
    
    /* maps TransactionIDs to the PageIDs of all pages that it called getPage for */ 
    private HashMap<TransactionId, HashSet<PageId>> transactionmap;
    
    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    
    public BufferPool(int numPages) {
        pagemap = new HashMap<PageId, Page>();
        this.numPages = numPages;
        timemap = new TreeMap<Long, Page>();
        transactionmap = new HashMap<TransactionId, HashSet<PageId>>();
        lm = new LockManager();
        currpages = 0;
    }
    
    public int numPages() {
    	return numPages;
    }
    public static LockManager getLockManager() { return lm; }
    

    public static int getPageSize() {
        return pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p/>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid  the ID of the transaction requesting the page
     * @param pid  the ID of the requested page
     * @param perm the requested permissions on the page
     * @throws IOException 
     */
    public Page getPage(TransactionId tid, PageId pid, Permissions perm)
            throws TransactionAbortedException, DbException {
    	
    	BufferPool.getLockManager().acquireLock(pid, tid, perm);
    	
    	synchronized(this) {

    		HashSet<PageId> set = null;

    		if (!transactionmap.containsKey(tid)) {
    			set = new HashSet<PageId>();
    		}

    		else {
    			set = transactionmap.get(tid);
    		}

    		set.add(pid);

    		transactionmap.put(tid, set);

    		if (pagemap.containsKey(pid)){
    			Long time = System.currentTimeMillis();
    			Page page = pagemap.get(pid);
    			timemap.put(time, page);
    			return page;
    		}

    		if (this.currpages == this.numPages) {
    			this.evictPage();
    		}

    		int tableid = pid.getTableId();
    		DbFile dbfile = Database.getCatalog().getDatabaseFile(tableid);
    		this.pagemap.put(pid, dbfile.readPage(pid));
    		Page page = pagemap.get(pid);
    		Long time = System.currentTimeMillis();
    		timemap.put(time, page);
    		currpages++;

    		return page;
    	}
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public void releasePage(TransactionId tid, PageId pid) {
        
    	try {
			BufferPool.getLockManager().releaseLock(pid, tid);
		} catch (TransactionAbortedException e) {
		}
    
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public synchronized void transactionComplete(TransactionId tid) throws IOException {
        
    	transactionComplete(tid, true);
    	
    
    }

    /**
     * Return true if the specified transaction has a lock on the specified page
     */
    public boolean holdsLock(TransactionId tid, PageId p) {
        
    	if (BufferPool.getLockManager().lockHeldBy(p) != null) {
    		if (BufferPool.getLockManager().lockHeldBy(p).contains(tid)) {
    			return true;
    		}
    	}
    	
    	return false;
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid    the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public synchronized void transactionComplete(TransactionId tid, boolean commit)
            throws IOException {
    	
    	if (transactionmap.get(tid) != null) {
    		if (commit) {
    			HashSet<PageId> pageset = transactionmap.get(tid);
    			for (PageId pageid:pageset) {
    				Page page;
					try {
						page = Database.getBufferPool().getPage(tid, pageid, Permissions.READ_ONLY);
						page.setBeforeImage();
						pagemap.put(pageid, page);
					} catch (TransactionAbortedException e) {
						e.printStackTrace();
					} catch (DbException e) {
						
					}
    			}
    			
    		}
    		HashSet<PageId> pageset = transactionmap.get(tid);
    		if (!commit) {
    			for (PageId pid:pageset) {
    				discardPage(pid);
    			}
    		}
    		for (PageId pid:pageset) {
    			if (BufferPool.getLockManager().lockHeldBy(pid).contains(tid)) {
    				releasePage(tid, pid);
    			}
    		}
    		BufferPool.getLockManager().removeFromWaiting(tid);
    		transactionmap.remove(tid);
    	}
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other
     * pages that are updated (Lock acquisition is not needed until lab5).                                  // cosc460
     * May block if the lock(s) cannot be acquired.
     * <p/>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and updates cached versions of any pages that have
     * been dirtied so that future requests see up-to-date pages.
     *
     * @param tid     the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t       the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
       DbFile file = Database.getCatalog().getDatabaseFile(tableId); 
       ArrayList<Page> pagelist = file.insertTuple(tid, t);
       for (Page page : pagelist) {
    	   page.markDirty(true, tid);
    	   pagemap.put(page.getId(), page);
    	   
       }
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     * <p/>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and updates cached versions of any pages that have
     * been dirtied so that future requests see up-to-date pages.
     *
     * @param tid the transaction deleting the tuple.
     * @param t   the tuple to delete
     */
    public void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
    	int tableId = t.getRecordId().getPageId().getTableId();
    	DbFile file = Database.getCatalog().getDatabaseFile(tableId);
    	ArrayList<Page> pagelist = file.deleteTuple(tid, t);
    	for (Page page : pagelist) {
     	   page.markDirty(true, tid);
     	   pagemap.put(page.getId(), page);
        }
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     * break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        for (PageId pageid : pagemap.keySet()) {
        	this.flushPage(pageid);
        }

    }

    /**
     * Remove the specific page id from the buffer pool.
     * Needed by the recovery manager to ensure that the
     * buffer pool doesn't keep a rolled back page in its
     * cache.
     */
    public synchronized void discardPage(PageId pid) {
    	/*Long key = null;
    	for (Entry<Long, Page> entry:timemap.entrySet()) {
    		if (entry.getValue().getId().equals(pid)) {
    			key = entry.getKey();
    			System.out.println(entry.getKey());
    			System.out.println(key);
    			break;
    		}
    	}
    	timemap.remove(key);*/
    	pagemap.remove(pid);
    	this.currpages--;

    	
    }

    /**
     * Flushes a certain page to disk
     *
     * @param pid an ID indicating the page to flush
     */
    private synchronized void flushPage(PageId pid) throws IOException {
        int tableId = pid.getTableId();
        DbFile file = Database.getCatalog().getDatabaseFile(tableId);
        Page page = pagemap.get(pid);
        if (page != null) {
        	TransactionId dirtier = page.isDirty();
            if (dirtier != null){
              Database.getLogFile().logWrite(dirtier, page.getBeforeImage(), page);
              Database.getLogFile().force();
              page.markDirty(false, dirtier);
              file.writePage(page);
              pagemap.put(pid, page); // update hashmap
            }
        	/*file.writePage(page);
        	TransactionId tid = new TransactionId();
        	page.markDirty(false, tid);
        	pagemap.put(pid, page);*/ // update hashmap
        }
    }

    /**
     * Write all pages of the specified transaction to disk.
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
    	HashSet<PageId> pageset = transactionmap.get(tid);
    	if (pageset != null) {
    		for (PageId pid:pageset) {
    			flushPage(pid);
    		}
    	}
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     * @throws IOException 
     */
    private synchronized void evictPage() throws DbException {
        assert (this.currpages == this.numPages); //shouldn't need to evict if there's still space
        if (timemap.firstEntry().getValue().isDirty() != null) {
        	try {
				flushPage(timemap.firstEntry().getValue().getId());
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        pagemap.remove(timemap.firstEntry().getValue().getId());
        timemap.remove(timemap.firstKey());
        currpages--;
    }

}
