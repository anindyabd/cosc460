package simpledb;

import java.io.IOException;

public class Lab3Main {

	    public static void main(String[] argv) 
	       throws DbException, TransactionAbortedException, IOException {

	        System.out.println("Loading schema from file:");
	        // file named college.schema must be in mysimpledb directory
	        Database.getCatalog().loadSchema("college.schema");

	        TransactionId tid = new TransactionId();
	        
	        SeqScan scanStudents = new SeqScan(tid, Database.getCatalog().getTableId("students"));
	        SeqScan scanTakes = new SeqScan(tid, Database.getCatalog().getTableId("takes"));
	        SeqScan scanProfs = new SeqScan(tid, Database.getCatalog().getTableId("profs"));
	        
	        JoinPredicate p = new JoinPredicate(0, Predicate.Op.EQUALS, 0);
	        Join joinStudentsAndTakes = new Join(p, scanStudents, scanTakes);
	        
	        JoinPredicate p1 = new JoinPredicate(3, Predicate.Op.EQUALS, 2);
	        Join joinAllThree = new Join(p1, joinStudentsAndTakes, scanProfs);
	        
	        StringField hay = new StringField("hay", 3);
	        Predicate filterpredicate1 = new Predicate(5, Predicate.Op.EQUALS, hay);
	        Filter f = new Filter(filterpredicate1, joinAllThree);
	        // query execution: we open the iterator of the root and iterate through results
	        System.out.println("Query results:");
	        f.open();
	        while (f.hasNext()) {
	            Tuple tup = f.next();
	            System.out.println("\t"+tup);
	        }
	        f.close();
	        Database.getBufferPool().transactionComplete(tid);
	    }

	}

