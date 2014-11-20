package simpledb;
import java.util.*;

public class LockManager {
	
	private volatile HashMap<PageId, HashSet<TransactionId>> lockheldmap;
	private volatile HashSet<PageId> writelockset;
	
	public LockManager() { 
		 
		/* maps the PageIds of the pages to HashSets containing the TransactionIds of all the 
		 * transactions that hold a lock on this page.
		 */
		lockheldmap = new HashMap<PageId, HashSet<TransactionId>>();
		
		/*
		 * holds the pages that have write locks on them.
		 */
		writelockset = new HashSet<PageId>();
	}
	
	public boolean acquireLock(PageId pid, TransactionId tid, Permissions perm) throws TransactionAbortedException {
		
		if (writelockset.contains(pid)) {
			if (!lockheldmap.get(pid).contains(tid)) {
				return false;
			}
		}
		if (perm.equals(Permissions.READ_WRITE)) {
			if (lockheldmap.get(pid) != null) {
				if (!lockheldmap.get(pid).contains(tid) || lockheldmap.get(pid).size()>1) {
					return false;
				}
			}
		}
		
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
