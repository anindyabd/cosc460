package simpledb;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TableStats represents statistics (e.g., histograms) about base tables in a
 * query.
 * <p/>
 * This class is not needed in implementing lab1|lab2|lab3.                                                   // cosc460
 */
public class TableStats {

    private static final ConcurrentHashMap<String, TableStats> statsMap = new ConcurrentHashMap<String, TableStats>();

    static final int IOCOSTPERPAGE = 1000;

    public static TableStats getTableStats(String tablename) {
        return statsMap.get(tablename);
    }

    public static void setTableStats(String tablename, TableStats stats) {
        statsMap.put(tablename, stats);
    }

    public static void setStatsMap(HashMap<String, TableStats> s) {
        try {
            java.lang.reflect.Field statsMapF = TableStats.class.getDeclaredField("statsMap");
            statsMapF.setAccessible(true);
            statsMapF.set(null, s);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, TableStats> getStatsMap() {
        return statsMap;
    }

    public static void computeStatistics() {
        Iterator<Integer> tableIt = Database.getCatalog().tableIdIterator();

        System.out.println("Computing table stats.");
        while (tableIt.hasNext()) {
            int tableid = tableIt.next();
            TableStats s = new TableStats(tableid, IOCOSTPERPAGE);
            setTableStats(Database.getCatalog().getTableName(tableid), s);
        }
        System.out.println("Done.");
    }

    /**
     * Number of bins for the histogram. Feel free to increase this value over
     * 100, though our tests assume that you have at least 100 bins in your
     * histograms.
     */
    static final int NUM_HIST_BINS = 100;
    int ioCostPerPage = IOCOSTPERPAGE;
    int tableid;
    int numpages;
    int totaltuples;
    TupleDesc td;
	HashMap<String, IntHistogram> intHistogramMap;
	HashMap<String, StringHistogram> stringHistogramMap;
	HashMap<Integer, HashSet<Object>> distinctValsMap;

    /**
     * Create a new TableStats object, that keeps track of statistics on each
     * column of a table
     *
     * @param tableid       The table over which to compute statistics
     * @param ioCostPerPage The cost per page of IO. This doesn't differentiate between
     *                      sequential-scan IO and disk seeks.
     */
    public TableStats(int tableid, int ioCostPerPage) {
    	this.tableid = tableid;
    	this.ioCostPerPage = ioCostPerPage;
    	HeapFile file = (HeapFile) Database.getCatalog().getDatabaseFile(tableid);
    	HashMap<String, Object[]> minmax = findminandmax(file);
    	this.numpages = file.numPages();
    	this.td = file.getTupleDesc();
    	TransactionId tid = new TransactionId();
    	DbFileIterator iterator = file.iterator(tid); 
    	this.intHistogramMap = new HashMap<String, IntHistogram>();
    	this.stringHistogramMap = new HashMap<String, StringHistogram>();
    	this.distinctValsMap = new HashMap<Integer, HashSet<Object>>();
    	this.totaltuples = 0;
    	try {
    		iterator.open();
			if (!iterator.hasNext()) {
				throw new RuntimeException("The table is empty.");
			}
			while(iterator.hasNext()){
				Tuple t = iterator.next();
				totaltuples++;
				for (int i = 0; i < td.numFields(); i++) {
					HashSet<Object> newset;
					if (distinctValsMap.get(i) == null) {
						newset = new HashSet<Object>();
					}
					else {
						newset = distinctValsMap.get(i);
					}
					newset.add(t.getField(i));
					distinctValsMap.put(i, newset);
					String fieldname = t.getTupleDesc().getFieldName(i);
					if (t.getField(i).getType().equals(Type.INT_TYPE)) {
						IntField thisfield = (IntField) t.getField(i);
						int thisint = thisfield.getValue();
						if (intHistogramMap.get(fieldname) == null) {
							int min = (int) minmax.get(fieldname)[0];
							int max = (int) minmax.get(fieldname)[1];
							IntHistogram thisFieldHist = new IntHistogram(NUM_HIST_BINS, min, max);
							thisFieldHist.addValue(thisint);
							intHistogramMap.put(fieldname, thisFieldHist);
						}
						else {
							IntHistogram thisFieldHist = intHistogramMap.get(fieldname);
							thisFieldHist.addValue(thisint);
							intHistogramMap.put(fieldname, thisFieldHist);
						}
					}
					else {
						StringField thisfield = (StringField) t.getField(i);
						String thisstring = thisfield.getValue();
						if (stringHistogramMap.get(fieldname) == null) {
							StringHistogram thisFieldHist = new StringHistogram(NUM_HIST_BINS);
							thisFieldHist.addValue(thisstring);
							stringHistogramMap.put(fieldname, thisFieldHist);
						}
						else {
							StringHistogram thisFieldHist = stringHistogramMap.get(fieldname);
							thisFieldHist.addValue(thisstring);
							stringHistogramMap.put(fieldname, thisFieldHist);
						}
					}
					
				}
			}
			iterator.rewind();
			iterator.close();
		} catch (DbException | TransactionAbortedException e) {
			e.printStackTrace();
		}
    	
	}
    
    private HashMap<String, Object[]> findminandmax(HeapFile file) {
    	TupleDesc td = file.getTupleDesc();
    	TransactionId tid = new TransactionId();
    	DbFileIterator iterator = file.iterator(tid);
    	HashMap<String, Object[]> returnmap = new HashMap<String, Object[]>();
    	try {
    		iterator.open();
			if (!iterator.hasNext()) {
				throw new RuntimeException("The table is empty.");
			}
			while(iterator.hasNext()){
				Tuple t = iterator.next();
				for (int i = 0; i < td.numFields(); i++) {
					String fieldname = t.getTupleDesc().getFieldName(i);
					if (t.getField(i).getType().equals(Type.INT_TYPE)) {
						IntField thisfield = (IntField) t.getField(i);
						int thisint = thisfield.getValue();
						if (returnmap.get(fieldname) == null) {
							Object[] minmax = {thisint, thisint};
							returnmap.put(fieldname, minmax);
						}
						else {
							Object[] minmax = returnmap.get(fieldname);
							int min = (int) minmax[0];
							int max = (int) minmax[1];
							if (thisint < min) {
								min = thisint;
							}
							if (thisint > max) {
								max = thisint;
							}
							minmax[0] = min;
							minmax[1] = max;
							returnmap.put(fieldname, minmax);
						}
					}
					
				}
			}
			iterator.rewind();
			iterator.close();
			return returnmap;
    	}
    	catch (DbException | TransactionAbortedException e) {
			e.printStackTrace();
		}
    	return null;
    }
    	

    /**
     * Estimates the cost of sequentially scanning the file, given that the cost
     * to read a page is costPerPageIO. You can assume that there are no seeks
     * and that no pages are in the buffer pool.
     * <p/>
     * Also, assume that your hard drive can only read entire pages at once, so
     * if the last page of the table only has one tuple on it, it's just as
     * expensive to read as a full page. (Most real hard drives can't
     * efficiently address regions smaller than a page at a time.)
     *
     * @return The estimated cost of scanning the table.
     */
    public double estimateScanCost() {
    	return numpages * ioCostPerPage; 
   }

    /**
     * This method returns the number of tuples in the relation, given that a
     * predicate with selectivity selectivityFactor is applied.
     *
     * @param selectivityFactor The selectivity of any predicates over the table
     * @return The estimated cardinality of the scan with the specified
     * selectivityFactor
     */
    public int estimateTableCardinality(double selectivityFactor) {
    	return (int) Math.ceil(totaltuples*selectivityFactor);
    }


    /**
     * This method returns the number of distinct values for a given field.
     * If the field is a primary key of the table, then the number of distinct
     * values is equal to the number of tuples.  If the field is not a primary key
     * then this must be explicitly calculated.  Note: these calculations should
     * be done once in the constructor and not each time this method is called. In
     * addition, it should only require space linear in the number of distinct values
     * which may be much less than the number of values.
     *
     * @param field the index of the field
     * @return The number of distinct values of the field.
     */
    public int numDistinctValues(int field) {
    	return distinctValsMap.get(field).size();

    }

    /**
     * Estimate the selectivity of predicate <tt>field op constant</tt> on the
     * table.
     *
     * @param field    The field over which the predicate ranges
     * @param op       The logical operation in the predicate
     * @param constant The value against which the field is compared
     * @return The estimated selectivity (fraction of tuples that satisfy) the
     * predicate
     */
   public double estimateSelectivity(int field, Predicate.Op op, Field constant) {
    	if(td.getFieldType(field).equals(Type.INT_TYPE)){
    		if(constant.getType() != Type.INT_TYPE){
    			throw new RuntimeException("Constant type does not match predicate field type");
    		}
    		String fieldname = td.getFieldName(field);
    		IntField constantfield = (IntField) constant;
    		int constantvalue = constantfield.getValue();
    		double selectivity = intHistogramMap.get(fieldname).estimateSelectivity(op, constantvalue);
    		return selectivity;
    	}
    	else{
    		if(constant.getType() != Type.STRING_TYPE){
    			throw new RuntimeException("Constant type does not match predicate field type");
    		}
    		String fieldname = td.getFieldName(field);
    		StringField constantfield = (StringField) constant;
    		String constantvalue = constantfield.getValue();
    		double selectivity = stringHistogramMap.get(fieldname).estimateSelectivity(op, constantvalue);
    		return selectivity;
    	}
   }

    } 


