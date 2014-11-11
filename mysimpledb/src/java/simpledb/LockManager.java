package simpledb;
import java.util.*;

public class LockManager {
	
	HashMap<PageId,LinkedList<TransactionId>> locktable;
	HashMap<PageId, TransactionId> lockheldmap;
	
	public LockManager() {
		locktable = new HashMap<PageId, LinkedList<TransactionId>>();
		lockheldmap = new HashMap<PageId, TransactionId>();
	}
	
	public void acquireLock(PageId pid, TransactionId tid) {
		boolean waiting = true;
		while (waiting) {
			if (lockheldmap.containsKey(pid)) {
				synchronized (this) {
					if (locktable.containsKey(pid)) {
						LinkedList<TransactionId> waitinglist = locktable.get(pid);
						waitinglist.add(tid);
						locktable.put(pid, waitinglist);
					}
					else {
						LinkedList<TransactionId> waitinglist = new LinkedList<TransactionId>();
						waitinglist.add(tid);
						locktable.put(pid, waitinglist);
					}
				}
				if (lockheldmap.containsKey(pid)) {
					try {
						Thread.sleep(1);
					}
					catch (InterruptedException e) {}
				}
			}
			else {
				synchronized (this) {
					waiting = false;
					lockheldmap.put(pid, tid);
					if (!locktable.containsKey(pid)) {
						LinkedList<TransactionId> waitinglist = new LinkedList<TransactionId>();
						waitinglist.add(tid);
						locktable.put(pid, waitinglist);
					}
					if (!locktable.get(pid).contains(tid)) {
						locktable.get(pid).addFirst(tid);
					}
				}
			}
		}
	}	
	
	public synchronized void releaseLock(PageId pid, TransactionId tid) {
		lockheldmap.remove(pid);
		locktable.get(pid).remove(tid);
		if (locktable.get(pid).isEmpty()) {
			locktable.remove(pid);
		}
		if (locktable.containsKey(pid)) {
			LinkedList<TransactionId> waitinglist = locktable.get(pid);
			TransactionId nexttid = waitinglist.poll();
			this.acquireLock(pid, nexttid);
		}
	}
	
	public TransactionId lockHeldBy(PageId pid) {
		if (!lockheldmap.containsKey(pid)) { 
			return null; 
		}
		return lockheldmap.get(pid);
		
	}
	
	
	
	
}
