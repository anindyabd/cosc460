package simpledb;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author mhay
 */
class LogFileRecovery {

	private final RandomAccessFile readOnlyLog;

	/**
	 * Helper class for LogFile during rollback and recovery.
	 * This class given a read only view of the actual log file.
	 *
	 * If this class wants to modify the log, it should do something
	 * like this:  Database.getLogFile().logAbort(tid);
	 *
	 * @param readOnlyLog a read only copy of the log file
	 */
	public LogFileRecovery(RandomAccessFile readOnlyLog) {
		this.readOnlyLog = readOnlyLog;
	}

	/**
	 * Print out a human readable representation of the log
	 */
	public void print() throws IOException {
		// since we don't know when print will be called, we can save our current location in the file
		// and then jump back to it after printing
		Long currentOffset = readOnlyLog.getFilePointer();

		readOnlyLog.seek(0);
		long lastCheckpoint = readOnlyLog.readLong(); // ignore this
		System.out.println("BEGIN LOG FILE");
		while (readOnlyLog.getFilePointer() < readOnlyLog.length()) {
			int type = readOnlyLog.readInt();
			long tid = readOnlyLog.readLong();
			switch (type) {
			case LogType.BEGIN_RECORD:
				System.out.println("<T_" + tid + " BEGIN>");
				break;
			case LogType.COMMIT_RECORD:
				System.out.println("<T_" + tid + " COMMIT>");
				break;
			case LogType.ABORT_RECORD:
				System.out.println("<T_" + tid + " ABORT>");
				break;
			case LogType.UPDATE_RECORD:
				Page beforeImg = LogFile.readPageData(readOnlyLog);
				Page afterImg = LogFile.readPageData(readOnlyLog);  // after image
				System.out.println("<T_" + tid + " UPDATE pid=" + beforeImg.getId() +">");
				break;
			case LogType.CLR_RECORD:
				afterImg = LogFile.readPageData(readOnlyLog);  // after image
				System.out.println("<T_" + tid + " CLR pid=" + afterImg.getId() +">");
				break;
			case LogType.CHECKPOINT_RECORD:
				int count = readOnlyLog.readInt();
				Set<Long> tids = new HashSet<Long>();
				for (int i = 0; i < count; i++) {
					long nextTid = readOnlyLog.readLong();
					tids.add(nextTid);
				}
				System.out.println("<T_" + tid + " CHECKPOINT " + tids + ">");
				break;
			default:
				throw new RuntimeException("Unexpected type!  Type = " + type);
			}
			long startOfRecord = readOnlyLog.readLong();   // ignored, only useful when going backwards thru log
		}
		System.out.println("END LOG FILE");

		// return the file pointer to its original position
		readOnlyLog.seek(currentOffset);

	}

	/**
	 * Rollback the specified transaction, setting the state of any
	 * of pages it updated to their pre-updated state.  To preserve
	 * transaction semantics, this should not be called on
	 * transactions that have already committed (though this may not
	 * be enforced by this method.)
	 *
	 * This is called from LogFile.recover after both the LogFile and
	 * the BufferPool are locked.
	 *
	 * @param tidToRollback The transaction to rollback
	 * @throws java.io.IOException if tidToRollback has already committed
	 */
	public void rollback(TransactionId tidToRollback) throws IOException {

		readOnlyLog.seek(0);
		long lastCheckpoint = readOnlyLog.readLong();
		readOnlyLog.seek(readOnlyLog.length());
		while (readOnlyLog.getFilePointer() > LogFile.LONG_SIZE) {
			readOnlyLog.seek(readOnlyLog.getFilePointer() - LogFile.LONG_SIZE);
			long recordstart = readOnlyLog.readLong();
			if (recordstart == -1) {
				recordstart = LogFile.LONG_SIZE;
			}
			readOnlyLog.seek(recordstart);
			int type = readOnlyLog.readInt();
			long tid = readOnlyLog.readLong();
			switch (type) {
			case LogType.BEGIN_RECORD:
            	if (tidToRollback.getId() == tid) {
            		return;
            	}
            	break;
			case LogType.UPDATE_RECORD:
				Page beforeImg = LogFile.readPageData(readOnlyLog);
				if (tidToRollback.getId() == tid) {
					DbFile file = Database.getCatalog().getDatabaseFile(beforeImg.getId().getTableId());
					file.writePage(beforeImg);
					Database.getBufferPool().discardPage(beforeImg.getId());
					Page afterImg = LogFile.readPageData(readOnlyLog);
					Database.getLogFile().logCLR(tid, beforeImg);
				}
				break;
			case LogType.COMMIT_RECORD:
				if (tid == tidToRollback.getId()) {
					throw new IOException("This transaction has already committed!");
				}
			default:
				if (type < 1 || type > 6) {
					throw new RuntimeException("Unexpected type!  Type = " + type);
				}
				else {
					break;
				}
			}
			readOnlyLog.seek(recordstart);
		}
		readOnlyLog.seek(readOnlyLog.length());
	}

	/**
	 * Recover the database system by ensuring that the updates of
	 * committed transactions are installed and that the
	 * updates of uncommitted transactions are not installed.
	 *
	 * This is called from LogFile.recover after both the LogFile and
	 * the BufferPool are locked.
	 */
	public void recover() throws IOException {
		//this.print();
		TreeSet<Long> losers = new TreeSet<Long>();
		HashSet<Long> undone = new HashSet<Long>();
		// redo phase
		readOnlyLog.seek(0);
		long lastCheckpoint = readOnlyLog.readLong(); 
		if (lastCheckpoint != -1) {
			System.out.println("checkpt");
			readOnlyLog.seek(lastCheckpoint);
		}
		while (readOnlyLog.getFilePointer() < readOnlyLog.length()) {
			int type = readOnlyLog.readInt();
			long tid = readOnlyLog.readLong();
			switch (type) {
			case LogType.BEGIN_RECORD:
				losers.add(tid);
				break;
			case LogType.COMMIT_RECORD:
				losers.remove(tid);
				break;
			case LogType.ABORT_RECORD:
				losers.remove(tid);
				break;
			case LogType.UPDATE_RECORD:
				Page beforeImg = LogFile.readPageData(readOnlyLog);
				Page afterImg = LogFile.readPageData(readOnlyLog);  
				DbFile file = Database.getCatalog().getDatabaseFile(afterImg.getId().getTableId());
				file.writePage(afterImg);
				Database.getBufferPool().discardPage(beforeImg.getId());
				//Database.getBufferPool().discardPage(afterImg.getId());

				break;
			case LogType.CLR_RECORD:
				afterImg = LogFile.readPageData(readOnlyLog); 
				DbFile file2 = Database.getCatalog().getDatabaseFile(afterImg.getId().getTableId());
				file2.writePage(afterImg);
				Database.getBufferPool().discardPage(afterImg.getId());
				break;
			case LogType.CHECKPOINT_RECORD:
				int count = readOnlyLog.readInt();
				for (int i = 0; i < count; i++) {
					losers.add(readOnlyLog.readLong());
				}
				break;
			default:
				throw new RuntimeException("Unexpected type!  Type = " + type);
			}
			long startOfRecord = readOnlyLog.readLong();
		}

		while (!losers.isEmpty()) {
			Long tid = losers.first();
			undo(tid);
			losers.remove(tid);
			undone.add(tid);
		}
		for (Long tid:undone) {
			Database.getLogFile().logAbort(tid);
		}
		readOnlyLog.seek(readOnlyLog.length());

	}

	private void undo(long tidToRollback) throws IOException {

		readOnlyLog.seek(0);
		long lastCheckpoint = readOnlyLog.readLong();
		readOnlyLog.seek(readOnlyLog.length());
		while (readOnlyLog.getFilePointer() > LogFile.LONG_SIZE) {
			readOnlyLog.seek(readOnlyLog.getFilePointer() - LogFile.LONG_SIZE);
			long recordstart = readOnlyLog.readLong();
			if (recordstart == -1) {
				recordstart = LogFile.LONG_SIZE;
			}
			readOnlyLog.seek(recordstart);
			int type = readOnlyLog.readInt();
			long tid = readOnlyLog.readLong();
			switch (type) {
			case LogType.BEGIN_RECORD:
				if (tid == tidToRollback) {
					return;
				}
				break;
			case LogType.UPDATE_RECORD:
				System.out.println("why undo?");
				Page beforeImg = LogFile.readPageData(readOnlyLog);
				Page afterImg = LogFile.readPageData(readOnlyLog);
				if (tidToRollback == tid) {
					DbFile file = Database.getCatalog().getDatabaseFile(beforeImg.getId().getTableId());
					file.writePage(beforeImg);
					Database.getBufferPool().discardPage(beforeImg.getId());
					Database.getLogFile().logCLR(tid, beforeImg);
				}
				break;
			case LogType.COMMIT_RECORD:
				if (tid == tidToRollback) {
					throw new IOException("This transaction has already committed!");
				}
			default:
				if (type < 1 || type > 6) {
					throw new RuntimeException("Unexpected type!  Type = " + type);
				}
				else {
					break;
				}
			}
			readOnlyLog.seek(recordstart);
		}

	}
}
