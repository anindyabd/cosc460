package simpledb;

import java.io.File;
import java.io.IOException;

public class Lab2Main {
	
	public static void main(String[] args) throws DbException, IOException, TransactionAbortedException {
		
		Type types[] = new Type[]{ Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE };
        String names[] = new String[]{ "field0", "field1", "field2" };
        TupleDesc descriptor = new TupleDesc(types, names);
        
		HeapFile table1 = new HeapFile(new File("some_data_file.dat"), descriptor);
		Database.getCatalog().addTable(table1, "test");
		
		TransactionId tid = new TransactionId();
        SeqScan f = new SeqScan(tid, table1.getId());
        
        Tuple t = new Tuple(descriptor);
        IntField ninety_nine = new IntField(99);
        
        t.setField(0, ninety_nine);
        t.setField(1, ninety_nine);
        t.setField(2, ninety_nine);
        
        table1.insertTuple(tid, t);
        
        try {
            // and run it
            f.open();
            while (f.hasNext()) {
                Tuple tup = f.next();
                IntField three = new IntField(3);
                if (!tup.getField(1).equals(three)) {
                	tup.setField(1, three);
                }
                System.out.println(tup);
            }
            f.close();
            Database.getBufferPool().flushAllPages();
            Database.getBufferPool().transactionComplete(tid);
        } catch (Exception e) {
            System.out.println ("Exception : " + e);
        }
	}
}
