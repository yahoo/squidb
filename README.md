[![Build Status](https://travis-ci.org/yahoo/squidb.svg?branch=master)](https://travis-ci.org/yahoo/squidb)

## Introducing SquiDB
SquiDB is a SQLite database layer for Android. It is designed to make it as easy as possible to work with SQLite databases while still enabling the power and flexibility of raw SQL. SquiDB combines typesafe objects that represent table rows with object-oriented SQL statement builders to make it easy to read and write your data without a bunch of messy SQL strings. It also includes built in tools and hooks to help you easily write database migrations as well as implement ContentProviders.

## Getting started

Add SquiDB to your existing project by following the instructions in [How to Set Up SquiDB in Android Studio](https://github.com/yahoo/squidb/wiki/How-to-set-up-SquiDB-in-Android-Studio). Below is a quick primer on the basics of SquiDB; please refer to [the wiki pages](ttps://github.com/yahoo/squidb/wiki) for more information about all the features of the library.

## Model objects
SquiDB represents rows in your SQLite tables as objects (similar to how an ORM might). Instead of directly defining these objects though, SquiDB uses compile time code generation to let you define your models/table schemas as minimally as possible--the actual code you will work with is generated at compile time. A DatabaseDao object mediates reading and writing these objects from the database. Setting up all these components is quick and easy. For example:

```java
// This is a table schema
@TableModelSpec(className = "Person", tableName = "people")
public class PersonSpec {

    // A text column named "firstName"
    public String firstName;

    // A text column named "lastName"
    public String lastName;

    // A long column named "creationDate", but referred to as "birthday"
    // when working with the model
    @ColumnSpec(name = "creationDate")
    public long birthday;
}

// This is how you'd set up a database instance
public class MyDatabase extends AbstractDatabase {

    private static final int VERSION = 1;

    public MyDatabase(Context context) {
        super(context);
    }

    @Override
    protected String getName() {
        return "my-database.db";
    }

    @Override
    protected Table[] getTables() {
        return new Table[]{
            // List all tables here
            Person.TABLE,
        };
    }

    @Override
    protected int getVersion() {
        return VERSION;
    }

    // Other overridable methods exist for migrations and initialization;
    // omitted for brevity
}

DatabaseDao dao = new DatabaseDao(new MyDatabase(context));

// This is how you'd work with the generated model
Person newPerson = new Person()
    .setFirstName("Sam")
    .setLastName("Bosley")
    .setBirthday(System.currentTimeMillis());
dao.persist(newPerson);

...

String firstName = newPerson.getFirstName();
String lastName = newPerson.getLastName();
long birthday = newPerson.getBirthday();
```

## Building queries
In addition to defining getters and setters for all the columns, the generated model class also defines constant fields you can reference for constructing queries:

```java
long ageCutoff = System.currentTimeMillis() - (DateUtil.YEAR_IN_MILLIS * 18);
Query peopleWhoCanVote = Query.select().where(Person.BIRTHDAY.lt(ageCutoff));

// This becomes select * from people where people.birthday < ?
// where ? is the age cutoff arg
SquidCursor<Person> voters = dao.query(Person.class, peopleWhoCanVote);
```

The example is simple, but SquiDB's query object supports almost the entire SQL grammar. It is much cleaner and easier to maintain, particularly for complex queries:
```java
String sql = "select " + PersonColumns.AGE + ", " + ProfileImageColumns.URL + " from "
    + PERSON_TABLE + " left join " + PROFILE_IMAGE_TABLE + " on " + PersonColumns._ID
    + " = " + ProfileImageColumns.PERSON_ID + " where " + PersonColumns.NAME + " = ?"
    + " AND " + PersonColumns.AGE + " >= ?" + " ORDER BY " + PersonColumns.AGE + " ASC"
String[] sqlArgs = new String[]{"Sam", Integer.toString(18)};

// Becomes...
Query query = Query.select(Person.AGE, ProfileImage.URL).from(Person.TABLE)
    .leftJoin(ProfileImage.TABLE, Person.ID.eq(ProfileImage.PERSON_ID))
    .where(Person.NAME.eq("Sam").and(Person.AGE.gte(18)));
```
The above example with strings uses the '?' character as placeholders for arguments to the statement. Users of Android's SQLiteDatabase will recognize this as the pattern used by many of its methods, including query methods. This is good practice, but it makes the code harder to read and necessitates that extra string array for the arguments. SquiDB inserts those placeholders for you when compiling the Query object and binds the arguments automatically at query time. The raw SQL version is also prone to errors when updating the SQL adds, removes, or changes the contents of sqlArgs. You must always count the number of '?'s to find the appropriate argument in the array. For large and complex queries, this can be difficult; SquiDB's Query object makes it a non-issue. Using SquiDB's Query also prevents several classes of typos (you won't ever mistype a keyword or forget a space character somewhere).

Furthermore, it becomes easier to build/compose queries or SQL clauses as objects:
```java
public Query queryForPeopleWithName(String name, boolean includeLastName) {
    Query baseQuery = Query.select().from(Person.TABLE);
    Criterion nameCriterion = Person.FIRST_NAME.eq(name);
    if (includeLastName) {
        nameCriterion = nameCriterion.or(Person.LAST_NAME.eq(name));
    }
    baseQuery.where(nameCriterion);
    return baseQuery;
}
```

## Working with query results
DatabaseDao can return either single rows of data represented by model objects, or a SquidCursor parametrized by a model type:
```java
// Fetch the person with _id = 1
Person person1 = dao.fetch(Person.class, 1);

// Cursor containing all rows in the people table
SquidCursor<Person> personCursor = dao.query(Person.class, Query.select());
```

Model objects are designed to be reusable, so iterating through the cursor and inflating model objects to work with is cheap if you don't need the row data to live outside of the loop:
```java
SquidCursor<Person> personCursor = dao.query(Person.class, Query.select());
try {
    Person person = new Person();
    while (personCursor.moveToNext()) {
        person.readPropertiesFromCursor(personCursor);
        doSomethingWithCurrentRow(person);
    }
} finally {
    personCursor.close();
}
```

SquidCursor is an instance of Android's CursorWrapper, so you can use one anywhere a standard Android Cursor is expected. It also provides users a typesafe get() method that can work directly with table columns if you don’t want or need to inflate a full model object:

```java
String firstName = personCursor.get(Person.FIRST_NAME);
Long birthday = personCursor.get(Person.BIRTHDAY);
```

These are simple examples that only use a single table, but it's still easy to work with model objects even if you need to join across multiple tables.

## And more!
We've shown several simple examples here, but there's a lot that SquiDB can do to make more complicated use cases easy too--it can help you work with SQL views using model objects, write database migrations, implement flexible ContentProviders backed by your SQLite database, and more. For an in-depth look at all you can do with SquiDB, check out the wiki at https://github.com/yahoo/squidb/wiki.

Code licensed under the Apache 2.0 license. See LICENSE file for terms.
