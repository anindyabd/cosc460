package simpledb;

import java.io.IOException;
import java.util.ArrayList;

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
	        
	        // join students and takes
	        int i = scanStudents.getTupleDesc().fieldNameToIndex("students.sid");
	        int j = scanTakes.getTupleDesc().fieldNameToIndex("takes.sid");
	        JoinPredicate p = new JoinPredicate(i, Predicate.Op.EQUALS, j);
	        Join joinStudentsAndTakes = new Join(p, scanStudents, scanTakes);
	        
	        // join all three tables
	        int m = joinStudentsAndTakes.getTupleDesc().fieldNameToIndex("takes.cid");
	        int n = scanProfs.getTupleDesc().fieldNameToIndex("profs.favoriteCourse");
	        JoinPredicate p1 = new JoinPredicate(m, Predicate.Op.EQUALS, n);
	        Join joinAllThree = new Join(p1, joinStudentsAndTakes, scanProfs);
	        
	        // select professor hay
	        StringField hay = new StringField("hay", 3);
	        int k = joinAllThree.getTupleDesc().fieldNameToIndex("profs.name");
	        Predicate filterpredicate1 = new Predicate(k, Predicate.Op.EQUALS, hay);
	        Filter f = new Filter(filterpredicate1, joinAllThree);
	        
	        // project out the names of the students
	        int nameindex = f.getTupleDesc().fieldNameToIndex("students.name");
	        ArrayList<Integer> fieldlist = new ArrayList<Integer>();
	        fieldlist.add(nameindex);
	        Type typeofname = Type.STRING_TYPE;
	        ArrayList<Type> typelist = new ArrayList<Type>();
	        typelist.add(typeofname);
	        Project project = new Project (fieldlist, typelist, f);
	        
	        // query execution: we open the iterator of the root and iterate through results
	        System.out.println("Query results:");
	        project.open();
	        while (project.hasNext()) {
	            Tuple tup = project.next();
	            System.out.println("\t"+tup);
	        }
	        project.close();
	        Database.getBufferPool().transactionComplete(tid);
	    }

	}

