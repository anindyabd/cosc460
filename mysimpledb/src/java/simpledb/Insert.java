package simpledb;

import java.io.IOException;

/**
 * Inserts tuples read from the child operator into the tableid specified in the
 * constructor
 */
public class Insert extends Operator {

    private static final long serialVersionUID = 1L;
    
    private TransactionId tid;
    private DbIterator child;
    private int tableid;
    private int numberofcalls;
    /**
     * Constructor.
     *
     * @param t       The transaction running the insert.
     * @param child   The child operator from which to read tuples to be inserted.
     * @param tableid The table in which to insert tuples.
     * @throws DbException if TupleDesc of child differs from table into which we are to
     *                     insert.
     */
    public Insert(TransactionId t, DbIterator child, int tableid)
            throws DbException {
    	TupleDesc tabletd = Database.getCatalog().getTupleDesc(tableid);
        if (!child.getTupleDesc().equals(tabletd)){
        	throw new DbException("TupleDescs don't match.");
        }
        this.tid = t;
        this.child = child;
        this.tableid = tableid;
        this.numberofcalls = 0;
        
    }

    public TupleDesc getTupleDesc() {
    	Type[] inttype = {Type.INT_TYPE}; 
        TupleDesc tdToReturn = new TupleDesc(inttype);
        return tdToReturn;
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
        this.numberofcalls = 0;
    }

    /**
     * Inserts tuples read from child into the tableid specified by the
     * constructor. It returns a one field tuple containing the number of
     * inserted records. Inserts should be passed through BufferPool. An
     * instances of BufferPool is available via Database.getBufferPool(). Note
     * that insert DOES NOT need check to see if a particular tuple is a
     * duplicate before inserting it.
     *
     * @return A 1-field tuple containing the number of inserted records, or
     * null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#insertTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        this.numberofcalls++;
        if (this.numberofcalls > 1) {
        	return null;
        }
    	int count = 0;
    	Tuple currtuple = null;
        if (child.hasNext()) {
        	currtuple = child.next();
        }
        
    	while (currtuple != null) {
        	try {
				Database.getBufferPool().insertTuple(this.tid, tableid, currtuple);
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
        Tuple tupleToReturn = new Tuple(this.getTupleDesc());
        IntField countfield = new IntField(count);
        tupleToReturn.setField(0, countfield);
        return tupleToReturn;
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
