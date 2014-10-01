package simpledb;

import java.io.IOException;


/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {

    private static final long serialVersionUID = 1L;
    
    private TransactionId tid;
    private DbIterator child;
    private int numberOfCalls;
    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     *
     * @param t     The transaction this delete runs in
     * @param child The child operator from which to read tuples for deletion
     */
    public Delete(TransactionId t, DbIterator child) {
        this.tid = t;
        this.child = child;
        this.numberOfCalls = 0;
    }

    public TupleDesc getTupleDesc() {
        Type[] inttype = {Type.INT_TYPE};
        TupleDesc td = new TupleDesc(inttype);
        return td;
    }

    public void open() throws DbException, TransactionAbortedException {
        child.open();
    	super.open();
    }

    public void close() {
    	child.close();
        super.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        child.rewind();
        this.numberOfCalls = 0;
    }

    /**
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method.
     *
     * @return A 1-field tuple containing the number of deleted records.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        this.numberOfCalls++;
        if (this.numberOfCalls > 1) {
        	return null;
        }
    	int count = 0;
        Tuple currtuple = null;
    	if (child.hasNext()) {
        	currtuple = child.next();
    	}
    	while (currtuple != null) {
        	try {
				Database.getBufferPool().deleteTuple(this.tid, currtuple);
				count++;
				if (child.hasNext()){
					currtuple = child.next();
				}
				else {
					currtuple = null;
				}
			} catch (IOException e) {
				throw new DbException("BufferPool threw IOException.");
			}
        	
        }
    	Tuple tupletoreturn = new Tuple(this.getTupleDesc());
    	IntField intfield = new IntField(count);
    	tupletoreturn.setField(0, intfield);
    	return tupletoreturn;
    }

    @Override
    public DbIterator[] getChildren() {
        DbIterator[] children = {this.child};
        return children;
    }

    @Override
    public void setChildren(DbIterator[] children) {
        this.child = children[0];
    }

}
