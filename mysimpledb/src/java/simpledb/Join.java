package simpledb;

import java.util.*;

/**
 * The Join operator implements the relational join operation.
 */
public class Join extends Operator {

    private static final long serialVersionUID = 1L;

    private DbIterator child1, child2;
    private JoinPredicate p;
    private Tuple child1tuple, child2tuple;
    /**
     * Constructor. Accepts two children to join and the predicate to join them
     * on
     *
     * @param p      The predicate to use to join the children
     * @param child1 Iterator for the left(outer) relation to join
     * @param child2 Iterator for the right(inner) relation to join
     * @throws TransactionAbortedException 
     * @throws DbException 
     * @throws NoSuchElementException 
     */
    public Join(JoinPredicate p, DbIterator child1, DbIterator child2) {
        this.child1 = child1;
        this.child2 = child2;
        this.p = p;
        child1tuple = null;
        child2tuple = null;
       
    }

    public JoinPredicate getJoinPredicate() {
        return p;
    }

    /**
     * @return the field name of join field1. Should be quantified by
     * alias or table name.
     */
    public String getJoinField1Name() {
        return child1.getTupleDesc().getFieldName(p.getField1());
    }

    /**
     * @return the field name of join field2. Should be quantified by
     * alias or table name.
     */
    public String getJoinField2Name() {
    	return child2.getTupleDesc().getFieldName(p.getField2());	
    }

    /**
     * @see simpledb.TupleDesc#merge(TupleDesc, TupleDesc) for possible
     * implementation logic.
     */
    public TupleDesc getTupleDesc() {
    	TupleDesc td1 = child1.getTupleDesc();
    	TupleDesc td2 = child2.getTupleDesc();
    	int new_length = td1.numFields() + td2.numFields();
    	Type[] typeAr = new Type[new_length]; 
    	String[] fieldAr = new String[new_length];
    	for (int i = 0; i < td1.numFields(); i++){
    		typeAr[i] = td1.getFieldType(i);
    		fieldAr[i] = td1.getFieldName(i);
    	}
    	for (int i = td1.numFields(), j = 0; i < new_length; i++, j++){
    		typeAr[i] = td2.getFieldType(j);
    		fieldAr[i] = td2.getFieldName(j);
    	}
    	TupleDesc newtd = new TupleDesc(typeAr, fieldAr);
        return newtd;
    }

    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        child1.open();
        child2.open();
        super.open();
    }

    public void close() {
        child1.close();
        child2.close();
        super.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
    	child1.rewind();
    	this.child1tuple = null;
        child2.rewind();
        this.child2tuple = null;
    }

    /**
     * Returns the next tuple generated by the join, or null if there are no
     * more tuples. Logically, this is the next tuple in r1 cross r2 that
     * satisfies the join predicate. There are many possible implementations;
     * the simplest is a nested loops join.
     * <p/>
     * Note that the tuples returned from this particular implementation of Join
     * are simply the concatenation of joining tuples from the left and right
     * relation. Therefore, if an equality predicate is used there will be two
     * copies of the join attribute in the results. (Removing such duplicate
     * columns can be done with an additional projection operator if needed.)
     * <p/>
     * For example, if one tuple is {1,2,3} and the other tuple is {1,5,6},
     * joined on equality of the first column, then this returns {1,2,3,1,5,6}.
     *
     * @return The next matching tuple.
     * @see JoinPredicate#filter
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
       if (child1tuple == null) {
    	   if (child1.hasNext()) {
    		   child1tuple = child1.next();
    	   }
    	   else {
    		   return null;
    	   }
       }
       if (child2tuple == null) {
    	   if (child2.hasNext()) {
    		   child2tuple = child2.next();
    	   }
    	   else {
    		   if (child1.hasNext()) {
    			   child2.rewind();
    			   child2tuple = child2.next();
    			   child1tuple = child1.next();
    		   }
    		   else {
    			   child1tuple = null;
    			   return null;
    		   }
    		   
    	   }
       }
       if (p.filter(child1tuple, child2tuple)) {
    	   Tuple joinedTuple = this.joinTuples(child1tuple, child2tuple);
    	   child2tuple = null;
    	   return joinedTuple;
       }
       else {
    	   while (child1tuple != null){
    		   while (child2tuple != null) {
    			   if (p.filter(child1tuple, child2tuple)) {
    				   Tuple joinedTuple = this.joinTuples(child1tuple, child2tuple);
    				   child2tuple = null;
    				   return joinedTuple;
    			   }
    			   if (child2.hasNext()) {
    				   child2tuple = child2.next();
    			   }
    			   else {
    				   child2tuple = null;
    			   }
    		}
    		   child2.rewind();
    		   child2tuple = child2.next();
    		   if (child1.hasNext()) {
    			   child1tuple = child1.next();
    		   }
    		   else {
    			   child1tuple = null;
    		   }
    	   }
    	   
    	   return null;
       }
    }
    
    private Tuple joinTuples(Tuple tuple1, Tuple tuple2){
    	TupleDesc td = this.getTupleDesc();
    	Tuple joinedtuple = new Tuple(td);
    	int i = 0;
    	while (i < tuple1.getTupleDesc().numFields()) {
    		joinedtuple.setField(i, tuple1.getField(i));
    		i++;
    	}
    	int j = 0;
    	while (j < tuple2.getTupleDesc().numFields()) {
    		joinedtuple.setField(i, tuple2.getField(j));
    		j++;
    		i++;
    	}
    	return joinedtuple;
    }

    @Override
    public DbIterator[] getChildren() {
        DbIterator[] children = {child1, child2};
    	return children;
    }

    @Override
    public void setChildren(DbIterator[] children) {
        child1 = children[0];
        child2 = children[1];
    }

}
