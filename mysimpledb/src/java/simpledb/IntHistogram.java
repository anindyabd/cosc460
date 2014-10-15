package simpledb;

import java.util.*;

import simpledb.Predicate.Op;
/**
 * A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {

		
	private int buckets, min, max, bucket_interval, last_bucket_key, total_tuples;
	private HashMap<Integer, Integer> bucketmap;
    /**
     * Create a new IntHistogram.
     * <p/>
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     * <p/>
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * <p/>
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't
     * simply store every value that you see in a sorted list.
     *
     * @param buckets The number of buckets to split the input value into.
     * @param min     The minimum integer value that will ever be passed to this class for histogramming
     * @param max     The maximum integer value that will ever be passed to this class for histogramming
     */
    public IntHistogram(int buckets, int min, int max) {
        this.buckets = buckets;
        if (buckets > max - min) {
        	buckets = max - min;
        	this.buckets = buckets;
        }
        this.min = min;
        this.max = max;
        int bucket_interval = (max - min) / buckets;
        int leftover = (max - min + 1) % buckets; 
        this.bucket_interval = bucket_interval;
        bucketmap = new HashMap<Integer, Integer>();
        for (int i = 0; i < buckets; i += bucket_interval) {
        	bucketmap.put(i, 0);
        }
        int last_bucket_key = max - bucket_interval - leftover;
        this.last_bucket_key = last_bucket_key;
    }

    /**
     * Add a value to the set of values that you are keeping a histogram of.
     *
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
    	if (v < min || v > max) {
        	throw new RuntimeException();
        }
    	total_tuples++;
        Boolean did_put = false;
        
        for (int i = 0; i < buckets; i += bucket_interval) {
        	if (v > i && (v-i) < bucket_interval) {
        		bucketmap.put(i, v);
        	}
        }
        //if the above didn't work, we add to the last bucket.
        if (did_put == false) {
        	bucketmap.put(last_bucket_key, v);
        }
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * <p/>
     * For example, if "op" is "GREATER_THAN" and "v" is 5,
     * return your estimate of the fraction of elements that are greater than 5.
     *
     * @param op Operator
     * @param v  Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {
        double h = -1;
        double w = -1;
        double selectivity = -1;
    	if (op.equals(Op.EQUALS) || op.equals(op.LIKE)) {
        	if (v <= max && v > last_bucket_key) {
        		h = last_bucket_key;
        		w = bucketmap.get(h);
        		selectivity = (h / w)/total_tuples;
        		return selectivity;
        	}
        	for (Integer value:bucketmap.keySet()) {
        		if (v > value && (v - value) < bucket_interval) {
        			h = v;
        			w = bucketmap.get(v);
        			break;
        		}
        	}
        	selectivity = (h / w)/total_tuples;
        	return selectivity;
        }
        return -1.0;
    }

    /**
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        // some code goes here
        return null;
    }
}
