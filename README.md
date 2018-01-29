[![Join the chat at https://gitter.im/yahoo/squidb](https://badges.gitter.im/yahoo/squidb.svg)](https://gitter.im/yahoo/squidb?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Build Status](https://travis-ci.org/yahoo/squidb.svg?branch=master)](https://travis-ci.org/yahoo/squidb) [![Download](https://api.bintray.com/packages/yahoo/maven/squidb/images/download.svg) ](https://bintray.com/yahoo/maven/squidb/_latestVersion)

Most ongoing development is currently taking place on the `dev_4.0` branch. Click [here](https://github.com/yahoo/squidb/tree/dev_4.0) to see the latest changes and try out the 4.0 beta.

## Introducing SquiDB
SquiDB is a cross-platform SQLite database layer for Android and iOS. It is designed to make it as easy as possible to work with SQLite databases while still enabling the power and flexibility of raw SQL. SquiDB combines typesafe objects that represent table rows with object-oriented SQL statement builders to make it easy to read and write your data without a bunch of messy SQL strings. It also includes built in tools and hooks to help you easily write database migrations as well as implement ContentProviders.

For support or to ask questions, join our chat channel on [gitter.im](https://gitter.im/yahoo/squidb).

## Getting started
To add SquiDB as a dependency in your build.gradle file:
```gradle
// This example is for a typical Android setup, j2objc/iOS setup may vary
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        // android-apt plugin; https://bitbucket.org/hvisser/android-apt
        // Only needed if using the android gradle plugin version less than 2.2.0
        // classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
    }
}

repositories {
    jcenter()
}

// Only needed if using the android gradle plugin version less than 2.2.0
// apply plugin: 'com.neenbedankt.android-apt'
// If using Kotlin, you may need to use this plugin instead of the default annotationProcessor configuration
// apply plugin: 'kotlin-kapt'

dependencies {
    compile 'com.yahoo.squidb:squidb:3.2.3'
    compile 'com.yahoo.squidb:squidb-annotations:3.2.3'
    compile 'com.yahoo.squidb:squidb-android:3.2.3' // For Android projects only
    annotationProcessor 'com.yahoo.squidb:squidb-processor:3.2.3'
    // If using the android-apt plugin, this becomes
    // apt 'com.yahoo.squidb:squidb-processor:3.2.3'
    // If using kotlin language, this becomes
    // kapt 'com.yahoo.squidb:squidb-processor:3.2.3'
}
```
See [this wiki page](https://github.com/yahoo/squidb/wiki/Adding-SquiDB-as-a-dependency) for more detailed instructions on adding SquiDB as a dependency.

#### A note on cross-platform support
As of SquiDB 3.0, cross-platform support is provided by compiling with [Google's j2objc tool](http://j2objc.org/). In other words, SquiDB can be used as a SQLite data layer to develop cross-platform business logic that will run on both Android and iOS platforms. If you don't need this feature, you can ignore it -- SquiDB will continue to work on Android exactly as it always has, with only minor, easy to update API changes.

#### Upgrading between major versions
* See [this wiki page](https://github.com/yahoo/squidb/wiki/Changes-in-SquiDB-2.0) if upgrading from version 1.x to 2.x
* See [this wiki page](https://github.com/yahoo/squidb/wiki/Changes-in-SquiDB-3.0) if upgrading from version 2.x to 3.x

## Model objects
SquiDB represents rows in your SQLite tables as objects (similar to how an ORM might). Instead of directly defining these objects though, SquiDB uses compile time code generation to let you define your models/table schemas as minimally as possible--the actual code you will work with is generated at compile time. A SquidDatabase object mediates reading and writing these objects from the database. Setting up all these components is quick and easy. For example:

```java
// This is a table schema
@TableModelSpec(className = "Person", tableName = "people")
class PersonSpec {

    // A text column named "firstName"
    String firstName;

    // A text column named "lastName"
    String lastName;

    // A long column named "creationDate", but referred to as "birthday"
    // when working with the model
    @ColumnSpec(name = "creationDate")
    long birthday;
}

// This is how you'd set up a database instance
public class MyDatabase extends SquidDatabase {

    private static final int VERSION = 1;

    public MyDatabase() {
        super();
        // Any other initialization of the instance
    }

    @Override
    public String getName() {
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

MyDatabase db = new MyDatabase(); // Important: db instances should always be singletons

// This is how you'd work with the generated model
Person newPerson = new Person()
    .setFirstName("Sam")
    .setLastName("Bosley")
    .setBirthday(System.currentTimeMillis());
db.persist(newPerson);

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
SquidCursor<Person> voters = db.query(Person.class, peopleWhoCanVote);
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
SquidDatabase can return either single rows of data represented by model objects, or a SquidCursor parametrized by a model type:
```java
// Fetch the person with _id = 1
Person person1 = db.fetch(Person.class, 1);

// Cursor containing all rows in the people table
SquidCursor<Person> personCursor = db.query(Person.class, Query.select());
```

Model objects are designed to be reusable, so iterating through the cursor and inflating model objects to work with is cheap if you don't need the row data to live outside of the loop:
```java
SquidCursor<Person> personCursor = db.query(Person.class, Query.select());
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

SquidCursor also provides users a typesafe get() method that can work directly with table columns if you don’t want or need to inflate a full model object:

```java
String firstName = personCursor.get(Person.FIRST_NAME);
Long birthday = personCursor.get(Person.BIRTHDAY);
```

These are simple examples that only use a single table, but it's still easy to work with model objects even if you need to join across multiple tables.

#### SquidCursor vs Android Cursor
SquidCursor implements a re-declaration of the Android Cursor interface, so you can use it in the same way you would use an Android cursor -- methods like `moveToFirst()`, `moveToNext()`, `isAfterLast()` etc. all work in exactly the same way as a standard Android cursor. If in an Android app you need an actual instance of android.database.Cursor, you can cast the result of SquidCursor.getCursor() like so:

```java
SquidCursor<Person> myCursor = ...;
Cursor androidCursor = (Cursor) myCursor.getCursor();
```

## Data change notifications
SquiDB supports listening for database changes and sending notifications or callbacks after write operations. The notification mechanism is extremely flexible and is customizable via user-defined objects subclassing `DataChangedNotifier`. DataChangedNotifier objects accumulate notifications based on metadata from the writes occurring during write operations or transactions (e.g. which tables have changed or which single row was updated). These notifications will then be sent if and only if the operation or transaction completes successfully.

Implementations of notifier objects that cover some of the most common use cases are provided:
* `UriNotifier` supports sending notifications to Uris for use with Android's ContentObserver mechanism. This can be useful when using SquiDB to implement a ContentProvider.
* `SimpleDataChangedNotifier` supports running an arbitrary callback after a successful write to the table or tables being listened to.
* For those who prefer Reactive architecture, the `squidb-reactive` module provides ReactiveSquidDatabase. ReactiveSquidDatabase is an extension of a standard SquidDatabase that supports creating RxJava Observables that will be notified when the given table(s) are written to.

See the [Listening for data changes](https://github.com/yahoo/squidb/wiki/Listening-for-data-changes) wiki page for examples of DataChangedNotifier, or the [Observing with RxJava](https://github.com/yahoo/squidb/wiki/Observing-with-RxJava) for examples of how to use the `squidb-reactive` module.

## And more!
We've shown several simple examples here, but there's a lot that SquiDB can do to make more complicated use cases easy too--it can help you work with SQL views using model objects, write database migrations, implement flexible ContentProviders backed by your SQLite database, and more. For an in-depth look at all you can do with SquiDB, check out the wiki at https://github.com/yahoo/squidb/wiki.

Code licensed under the Apache 2.0 license. See LICENSE file for terms.
