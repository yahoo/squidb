package com.yahoo.squidb.test;

public class SquidbBasicTests {

    private final TestDatabase db;
    
    public SquidbBasicTests() {
        String path = getPath();
        System.err.println("Database path " + path);
        db = new TestDatabase(path);
        db.clear();
        runTests();
    }
    
    private native String getPath() /*-[ 
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
                                     
        NSString * documentsDirectory = [paths objectAtIndex:0];
        return [documentsDirectory stringByAppendingPathComponent:@"/Databases"];
    ]-*/;
                                     
    
    private void runTests() {
        TestModel model = new TestModel()
        .setFirstName("Sam")
        .setLastName("Bosley")
        .setBirthday(System.currentTimeMillis())
        .setLuckyNumber(2);
        if (!db.persist(model)) {
            throw new RuntimeException("Persist model failed");
        } else {
            System.err.println("Persisted model");
        }
        
        TestModel fetchedModel = db.fetch(TestModel.class, model.getId());
        if (fetchedModel == null) {
            throw new NullPointerException("Fetched model was null");
        }
        
        if (!"Sam".equals(fetchedModel.getFirstName())) {
            throw new RuntimeException("First name mismatch, was " + fetchedModel.getFirstName());
        } else {
            System.err.println("First name matched");
        }
        
        if (!"Bosley".equals(fetchedModel.getLastName())) {
            throw new RuntimeException("Last name mismatch, was " + fetchedModel.getLastName());
        } else {
            System.err.println("Last name matched");
        }
        
        if (fetchedModel.getLuckyNumber() != 2) {
            throw new RuntimeException("Lucky number mismatch, was " + fetchedModel.getLuckyNumber());
        } else {
            System.err.println("Lucky number matched");
        }
        
        fetchedModel.setFirstName("New Sam");
        if (fetchedModel.getId() == 0) {
            throw new RuntimeException("ID was 0");
        } else {
            System.err.println("ID was non-zero, attempting update");
        }
        
        if (!db.persist(fetchedModel)) {
            throw new RuntimeException("Failed to update");
        } else {
            System.err.println("Updated model");
        }
        
        fetchedModel = db.fetch(TestModel.class, fetchedModel.getId());
        if (fetchedModel == null) {
            throw new NullPointerException("Fetched model v2 was null");
        }
        
        if (!"New Sam".equals(fetchedModel.getFirstName())) {
            throw new RuntimeException("Updated first name mismatch, was " + fetchedModel.getFirstName());
        } else {
            System.err.println("Updated first name matched");
        }
    }

}