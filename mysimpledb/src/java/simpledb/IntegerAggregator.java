package simpledb;

import java.util.ArrayList;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op what;
    private TupleDesc td;
    private ArrayList<Tuple> tuplelist;
    /**
     * Aggregate constructor
     *
     * @param gbfield     the 0-based index of the group-by field in the tuple, or
     *                    NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null
     *                    if there is no grouping
     * @param afield      the 0-based index of the aggregate field in the tuple
     * @param what        the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
    	this.gbfield = gbfield;
    	this.gbfieldtype = gbfieldtype;
    	this.afield = afield;
    	this.what = what;
    	if (gbfield == Aggregator.NO_GROUPING) {
    		Type[] typeArr = {Type.INT_TYPE};
    		String[] fieldArr = {"aggregateVal"};
    		TupleDesc tupledesc = new TupleDesc(typeArr, fieldArr);
    		this.td = tupledesc;
    	}
    	else {
    		Type[] typeArr = {Type.INT_TYPE, Type.INT_TYPE};
    		String[] fieldArr = {"groupVal", "aggregateVal"};
    		TupleDesc tupledesc = new TupleDesc(typeArr, fieldArr);
    		this.td = tupledesc;
    	}
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     *
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        IntField thisintfield = (IntField) tup.getField(this.afield);
    	if (this.td.numFields() == 1) {
    		
    	}
        
    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal, aggregateVal)
     * if using group, or a single (aggregateVal) if no grouping. The
     * aggregateVal is determined by the type of aggregate specified in
     * the constructor.
     */
    public DbIterator iterator() {
        // some code goes here
        throw new
        UnsupportedOperationException("please implement me for lab3");                                     // cosc460
    }

}
