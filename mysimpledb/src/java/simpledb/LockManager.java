package simpledb;
import java.util.*;

public class LockManager {
	
	private volatile HashMap<PageId,LinkedList<TransactionId>> locktable;
	private volatile HashMap<TransactionId, Permissions> waitingperms;
	private volatile HashMap<PageId, HashSet<TransactionId>> lockheldmap;
	private volatile HashMap<PageId, Boolean> writelockmap;
	
	public LockManager() {
		locktable = new HashMap<PageId, LinkedList<TransactionId>>();
		lockheldmap = new HashMap<PageId, HashSet<TransactionId>>();
		waitingperms = new HashMap<TransactionId, Permissions>();
		writelockmap = new HashMap<PageId, Boolean>();
	}
	
	public synchronized void acquireLock(PageId pid, TransactionId tid, Permissions perm) {
		boolean waiting = true;
		while (waiting) {
			if (writelockmap.containsKey(pid) && lockheldmap.get(pid).contains(tid)){
				break;
			}
			if ((writelockmap.get(pid) != null && writelockmap.get(pid)) || (perm == Permissions.READ_WRITE && locktable.containsKey(pid))) {
				if (locktable.containsKey(pid)) {
					LinkedList<TransactionId> waitinglist = locktable.get(pid);
					waitinglist.add(tid);
					locktable.put(pid, waitinglist);
					waitingperms.put(tid, perm);
				}
				else {
					LinkedList<TransactionId> waitinglist = new LinkedList<TransactionId>();
					waitinglist.add(tid);
					locktable.put(pid, waitinglist);
					waitingperms.put(tid, perm);
				}

				if (waiting) {
					try {
						Thread.sleep(1); // busy wait
					}
					catch (InterruptedException e) {}
				}
			}
			else {
				waiting = false;
			}
		}
		if (perm == Permissions.READ_WRITE) {
			writelockmap.put(pid, true);
		}
		else {
			writelockmap.put(pid, false);
		}
		if (lockheldmap.get(pid) == null) {
			HashSet<TransactionId> transactionset = new HashSet<TransactionId>();
			transactionset.add(tid);
			lockheldmap.put(pid, transactionset);
		}
		else {
			HashSet<TransactionId> transactionset = lockheldmap.get(pid);
			transactionset.add(tid);
			lockheldmap.put(pid, transactionset);
		}

		if (!locktable.containsKey(pid)) {
			LinkedList<TransactionId> waitinglist = new LinkedList<TransactionId>();
			waitinglist.add(tid);
			locktable.put(pid, waitinglist);
		}
		if (!locktable.get(pid).contains(tid)) {
			locktable.get(pid).addFirst(tid);
		}

	}

		
	
	public synchronized void releaseLock(PageId pid, TransactionId tid) {
		lockheldmap.remove(pid);
		locktable.get(pid).remove(tid);
		if (writelockmap.get(pid) != null) {
			writelockmap.remove(pid);
		}
		if (locktable.get(pid).isEmpty()) {
			locktable.remove(pid);
		}
		if (locktable.containsKey(pid)) {
			LinkedList<TransactionId> waitinglist = locktable.get(pid);
			TransactionId nexttid = waitinglist.poll();
			Permissions perm = waitingperms.get(nexttid);
			this.acquireLock(pid, nexttid, perm);
		}
	}
	
	public HashSet<TransactionId> lockHeldBy(PageId pid) {
		if (!lockheldmap.containsKey(pid)) { 
			return null; 
		}
		return lockheldmap.get(pid);
		
	}
	
	
	
	
}
