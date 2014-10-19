package simpledb;

import java.util.*;

import simpledb.Predicate.Op;
/**
 * A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {

		
	private int buckets, min, max, bucket_interval, total_tuples, remainder;
	private int[] bucketlist;  
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
        if (buckets > max - min + 1) {
        	buckets = max - min + 1;
        	this.buckets = buckets;
        }
        this.min = min;
        this.max = max;
        this.bucket_interval = (max - min + 1) / buckets;
        this.remainder = (max - min + 1) % buckets; 
        bucketlist = new int[buckets];
        for (int i = 0; i < buckets; i++) {
        	bucketlist[i] = 0;
        }
        this.total_tuples = 0;
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
        int bucketno = (v - this.min)/this.bucket_interval;
        if (bucketno >= buckets) {
        	bucketno = buckets - 1;
        }
        int count = bucketlist[bucketno];
        count++;
        bucketlist[bucketno] = count;
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
        double selectivity = -1;
        int bucketno = (v - this.min)/this.bucket_interval;
        double w = this.bucket_interval;
        if (bucketno >= buckets - 1) {
        	w = bucket_interval + remainder;
        	if (v <= this.max) {
        		bucketno = buckets -1;
        	}
        }
        double h = 0;
        if (v >= this.min && v <= this.max){
        	h = bucketlist[bucketno];
        }
        if (op.equals(Op.EQUALS) || op.equals(Op.LIKE)) {
        	if (v > max || v < min) {
        		selectivity = 0;
        	}
        	else {
        		selectivity = (h/w) / (double) this.total_tuples;
        	}
        }
        else if (op.equals(Op.NOT_EQUALS)) {
        	selectivity = (total_tuples - (h/w))/ (double) this.total_tuples;
        }
        else { // must be a range predicate
        	double b_f = h/ (double) total_tuples;
    		double b_right = min + (bucket_interval * (bucketno+1));
    		double b_part = (b_right -1 - v) / w;
    		if (bucketno == buckets - 1) {
    			b_part = (max - v) / w;
    		}
    		double b_selectivity = b_f * b_part;
        	
    		if (op.equals(Op.GREATER_THAN) || op.equals(Op.GREATER_THAN_OR_EQ)) {	
        		double sum_selectivity = 0;
        		int start_index = bucketno + 1;
        		if (start_index < 0) start_index = 0;
        		for (int i = start_index; i < buckets; i++){
        			double h_b = bucketlist[i];
        			sum_selectivity += h_b / total_tuples; 
        		}
        		selectivity = b_selectivity + sum_selectivity; 
        		if (op.equals(Op.GREATER_THAN_OR_EQ)) {
        			if (v == max) {
        				selectivity = (h/w) / (double) this.total_tuples;
        			}
        			else if (v == min) {
        				selectivity = 1;
        			}
        			else selectivity = selectivity + (h/w) / (double) this.total_tuples;
        		}

        	}
        	else { // must be less than or less than or equal to
        		double sum_selectivity = 0;
        		int start_index = 0;
        		if (bucketno > buckets) {
        			bucketno = buckets;
        		}
        		for (int i = 0; i < bucketno; i++) {
        			double h_b = bucketlist[i];
        			sum_selectivity += h_b / total_tuples;
        		}
        		selectivity = b_selectivity + sum_selectivity;
        		if (op.equals(Op.LESS_THAN_OR_EQ)){
        			if (v == min) {
        				selectivity = (h/w) / (double) this.total_tuples;
        			}
        			else if (v == max) {
        				selectivity = 1;
        			}
        			else selectivity = selectivity + (h/w) / (double) this.total_tuples;
        		}
        	}
        }
        
        
    	return selectivity;
    }

    /**
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        String to_return = "max:" + this.max + " min: " + this.min;
        for (int i = 0; i < buckets; i++) {
        	to_return += " bucket # " + i + ":" + bucketlist[i] + ";";
        }
        return to_return;
    }
}
