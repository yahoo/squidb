//package com.yahoo.squidb.test;
//
//public class SquidbBasicTests {
//
//    private final TestDatabase db;
//    
//    public SquidbBasicTests() {
//        String path = getPath();
//        db = new TestDatabase(path);
//        db.clear();
//        runTests();
//        testDatabaseWriteWithNullCharactersWorks();
//    }
//    
//    private native String getPath() /*-[ 
//        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
//                                     
//        NSString * documentsDirectory = [paths objectAtIndex:0];
//        return [documentsDirectory stringByAppendingPathComponent:@"/Databases"];
//    ]-*/;
//                                     
//    
//    private void runTests() {
//        TestModel model = new TestModel()
//        .setFirstName("Sam")
//        .setLastName("Bosley")
//        .setBirthday(System.currentTimeMillis())
//        .setLuckyNumber(2);
//        if (!db.persist(model)) {
//            throw new RuntimeException("Persist model failed");
//        } else {
//            System.err.println("Persisted model");
//        }
//        
//        TestModel fetchedModel = db.fetch(TestModel.class, model.getId());
//        if (fetchedModel == null) {
//            throw new NullPointerException("Fetched model was null");
//        }
//        
//        if (!"Sam".equals(fetchedModel.getFirstName())) {
//            throw new RuntimeException("First name mismatch, was " + fetchedModel.getFirstName());
//        } else {
//            System.err.println("First name matched");
//        }
//        
//        if (!"Bosley".equals(fetchedModel.getLastName())) {
//            throw new RuntimeException("Last name mismatch, was " + fetchedModel.getLastName());
//        } else {
//            System.err.println("Last name matched");
//        }
//        
//        if (fetchedModel.getLuckyNumber() != 2) {
//            throw new RuntimeException("Lucky number mismatch, was " + fetchedModel.getLuckyNumber());
//        } else {
//            System.err.println("Lucky number matched");
//        }
//        
//        fetchedModel.setFirstName("New Sam");
//        if (fetchedModel.getId() == 0) {
//            throw new RuntimeException("ID was 0");
//        } else {
//            System.err.println("ID was non-zero, attempting update");
//        }
//        
//        if (!db.persist(fetchedModel)) {
//            throw new RuntimeException("Failed to update");
//        } else {
//            System.err.println("Updated model");
//        }
//        
//        fetchedModel = db.fetch(TestModel.class, fetchedModel.getId());
//        if (fetchedModel == null) {
//            throw new NullPointerException("Fetched model v2 was null");
//        }
//        
//        if (!"New Sam".equals(fetchedModel.getFirstName())) {
//            throw new RuntimeException("Updated first name mismatch, was " + fetchedModel.getFirstName());
//        } else {
//            System.err.println("Updated first name matched");
//        }
//    }
//    
//    public void testDatabaseWriteWithNullCharactersWorks() {
//        testBadString("Sam\0B", 5);
//        testBadString("Sam\0\0B", 6);
//        testBadString("\0Sam\0B", 6);
//        testBadString("\0Sam\0B\0", 7);
//    }
//    
//    private void testBadString(String badString, int expectedLength) {
//        assertEquals(expectedLength, badString.length());
//        TestModel model = new TestModel().setFirstName(badString).setLastName("ABCDE").setBirthday(System.currentTimeMillis());
//        db.persist(model);
//        
//        model = db.fetch(TestModel.class, model.getId());
//        assertEquals(badString, model.getFirstName(), "Model first name match");
//        
//        db.update(TestModel.FIRST_NAME.in(badString), new TestModel().setFirstName("Sam"));
//        
//        model = db.fetch(TestModel.class, model.getId());
//        assertEquals("Sam", model.getFirstName(), "Updated model first name match");
//        db.delete(TestModel.class, model.getId());
//    }
//    
//    private void assertEquals(int expected, int actual) {
//        if (expected != actual) {
//            throw new RuntimeException("Expected " + expected + " but was " + actual);
//        }
//    }
//    
//    private void assertEquals(String expected, String actual, String successMessage) {
//        if (expected == null && actual != null) {
//            throw new RuntimeException("Expected null but got " + actual);
//        } else if (!expected.equals(actual)) {
//            throw new RuntimeException("Expected " + expected + " but got " + actual);
//        } else {
//            if (successMessage != null) {
//                System.err.println(successMessage);
//            }
//        }
//    }
//
//}