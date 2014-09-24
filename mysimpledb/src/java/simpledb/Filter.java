package simpledb;

import java.util.*;

/**
 * Filter is an operator that implements a relational select.
 */
public class Filter extends Operator {

    private static final long serialVersionUID = 1L;
    private Predicate p;
    private DbIterator child;
    /**
     * Constructor accepts a predicate to apply and a child operator to read
     * tuples to filter from.
     *
     * @param p     The predicate to filter tuples with
     * @param child The child operator
     */
    public Filter(Predicate p, DbIterator child) {
        this.p = p;
        this.child = child;    }

    public Predicate getPredicate() {
        return p;
    }

    public TupleDesc getTupleDesc() {
        return child.getTupleDesc();
    }

    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        this.child.open();
    }

    public void close() {
        this.child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
    	this.child.rewind();
    }

    /**
     * AbstractDbIterator.readNext implementation. Iterates over tuples from the
     * child operator, applying the predicate to them and returning those that
     * pass the predicate (i.e. for which the Predicate.filter() returns true.)
     *
     * @return The next tuple that passes the filter, or null if there are no
     * more tuples
     * @see Predicate#filter
     */
    protected Tuple fetchNext() throws NoSuchElementException,
            TransactionAbortedException, DbException {
    	child.open();
    	if (!child.hasNext()) {
        	throw new NoSuchElementException();
        }
        Tuple t = child.next();
    	while (child.hasNext()) {
        	if (p.filter(t)){
        		break;
        	}
        	t = child.next();
        }
    	
        return t;
    }

    @Override
    public DbIterator[] getChildren() {
        DbIterator[] children = {child};
        return children;
    }

    @Override
    public void setChildren(DbIterator[] children) {
        this.child = children[0];
    }

}