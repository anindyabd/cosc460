package simpledb;
import java.util.*;

public class LockManager {
    
    private volatile HashMap<PageId, HashSet<TransactionId>> lockheldmap;
    private volatile HashSet<PageId> writelockset;
    private volatile HashMap<PageId, LinkedList<TransactionId>> locktable;
    private volatile HashMap<TransactionId, HashMap<PageId, Long>> waitingtransactions;
    
    public LockManager() { 
         
        /* maps the PageIds of the pages to HashSets containing the TransactionIds of all the 
         * transactions that hold a lock on this page.
         */
        lockheldmap = new HashMap<PageId, HashSet<TransactionId>>();
        
        /*
         * holds the pages that have write locks on them.
         */
        writelockset = new HashSet<PageId>();
        
        /*
         * Maintains a FIFO queue of transactions waiting for each page
         */
        locktable = new HashMap<PageId, LinkedList<TransactionId>>();
        
        /*
         * Maps the transactions that are waiting to a map of 
         */
        waitingtransactions = new HashMap<TransactionId, HashMap<PageId, Long>>();
        
    }
    
    public boolean acquireLock(PageId pid, TransactionId tid, Permissions perm) throws TransactionAbortedException {
        
        synchronized (this) {
            HashMap<PageId, Long> pagemap;
            if (waitingtransactions.containsKey(tid)) {
                if (waitingtransactions.get(tid).containsKey(pid)) { // this txn is already waiting for this page
                    return false;
                }
                pagemap = waitingtransactions.get(tid);
            }
            else {
                pagemap = new HashMap<PageId, Long>();
            }
            Long time = System.currentTimeMillis();
            pagemap.put(pid, time);
            waitingtransactions.put(tid, pagemap);
        }
        
        while (true) { // keep spinning unless we break
            
            synchronized (this) { // sleeping is done OUTSIDE this block; synchronized is for all the checking and updating data structures
                
                // add the tid to FIFO queue
                
                LinkedList<TransactionId> queue;
                if (locktable.get(pid) != null) {
                    queue = locktable.get(pid);
                }
                else {
                    queue = new LinkedList<TransactionId>();
                }
                
                if (!waitingtransactions.containsKey(tid)) { // if this transaction was aborted/ completed, remove it from queue, and get out
                    while (queue.contains(tid)) {
                        queue.remove(tid);
                    }
                    locktable.put(pid, queue);
                    throw new TransactionAbortedException();
                }
                
                if (!queue.contains(tid)) { // if there's no record of this tid waiting for this page, add the record
                    queue.add(tid);
                }
                
                locktable.put(pid, queue);
                
                if (!waitingtransactions.containsKey(locktable.get(pid).peek())) {
                    locktable.get(pid).poll();
                }
                 
                if (locktable.get(pid).peek().equals(tid)) { // if this txn is first in line...
                    if (!lockheldmap.containsKey(pid) || lockheldmap.get(pid).isEmpty()) { // if there's no lock on this page, go ahead
                        break;
                    }
                    if (!writelockset.contains(pid) && perm.equals(Permissions.READ_ONLY)) { // if there's no X lock on this page
                        // and this txn just wants a shared lock, go ahead
                        break;
                    }
                    if (lockheldmap.containsKey(pid) && lockheldmap.get(pid).contains(tid) && writelockset.contains(pid)) { // this txn has a write lock on this page; go ahead 
                        break;
                    }
                    if (lockheldmap.containsKey(pid) && lockheldmap.get(pid).contains(tid) && lockheldmap.get(pid).size() == 1) { // this txn is the only one with a lock on this page, go ahead
                        break;
                    }
                    
                }
                
                Long currtime = System.currentTimeMillis();
                if (currtime - waitingtransactions.get(tid).get(pid) > 3000) { 
                    
                    throw new TransactionAbortedException();
                }
                
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        synchronized (this) {
            if (!locktable.containsKey(pid) || !waitingtransactions.containsKey(tid)) { // txn probably aborted
                throw new TransactionAbortedException();
            }
            if (!locktable.get(pid).peek().equals(tid)) { // breaking FIFO order! 
                throw new TransactionAbortedException();
            }
            locktable.get(pid).poll(); // remove the tid at the head of the queue, because it's leaving
            
            HashMap map = waitingtransactions.get(tid);
            map.remove(pid); //note that this txn is no longer waiting for this page
            waitingtransactions.put(tid, map);
            
            if (perm.equals(Permissions.READ_WRITE)) {
                writelockset.add(pid);
            }
            HashSet<TransactionId> transactionset = null;
            if (lockheldmap.get(pid) == null) {
                transactionset = new HashSet<TransactionId>();
            }
            else {
                transactionset = lockheldmap.get(pid);
            }
            transactionset.add(tid);
            lockheldmap.put(pid, transactionset);
            return true;
        }
    }

    /* releases the lock held on the page with PageId pid by Transaction with TransactionId tid */
    public synchronized void releaseLock(PageId pid, TransactionId tid) throws TransactionAbortedException {
        
        if (writelockset.contains(pid)) {
            writelockset.remove(pid);
        }
        HashSet<TransactionId> set = lockheldmap.get(pid);
        set.remove(tid);
        lockheldmap.put(pid, set);
        if (lockheldmap.get(pid).isEmpty()) {
            lockheldmap.remove(pid);
        }
    }
    
    public synchronized void removeFromWaiting(TransactionId tid) {
        waitingtransactions.remove(tid);
    }
    /**
     * 
     * @param pid
     * @return A HashSet of all the TransactionIds of the transactions 
     * that hold a lock on this page. If no transaction holds a lock 
     * on this page, null is returned.
     */
    public synchronized HashSet<TransactionId> lockHeldBy(PageId pid) {
        if (!lockheldmap.containsKey(pid)) { 
            return null; 
        }
        return lockheldmap.get(pid);
    }
    
    
}
