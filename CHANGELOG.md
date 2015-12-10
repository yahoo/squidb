Change Log
==========

Version 2.0.2 *(2015-12-10)*
---------------------------
* Adjustment to code generation so that constants are written after schema declaration. This allows constants to be defined in terms of Property objects in the generated model (e.g. `public static final Order DEFAULT_ORDER = Model.TIMESTAMP.asc()`). Also introduces the `@Constants` annotation, which allows annotating a static inner class of constants in a model spec for when class loading order is important (as it is in ViewModels).
* Add some utilities for logging the contents of a cursor in a formatted way (see `SquidUtilities.dumpCursor`)
* Bump the SQLite version in the `squidb-sqlite-bindings` project to 3.9.2
* Fix a bug where properties returned from `qualifyField` had an explicit alias set when they should not
* Fix a bug where tables aliased using `Table.as` did not have an associated rowid property

Version 2.0.1 *(2015-10-29)*
----------------------------
* SquidDatabase will now call onConfigure before onOpen for Android API level < 16
* Function.substr can now get its start and length arguments from arbitrary expressions, not just integers
* Support using ThreadLocals as arguments in SQL statements. This can make reusing a precompiled query or other SQL grammar object easier in a multi-threaded environment
* Add versions of Criterion.or and Criterion.and that work on List&lt;Criterion&gt;
* Bump the SQLite version in the `squidb-sqlite-bindings` project to 3.9.1
* Fixed an issue where SquidDatabase called insertOrThrow when wrapping SQLiteDatabase.insert
* Fix a bug in the code generator that could cause a necessary abstract method to be omitted

Version 2.0.0 *(2015-09-23)*
----------------------------
* Unified DatabaseDao and AbstractDatabase into a single SquidDatabase API
* Basic RxJava support via the `squidb-reactive` module
* Improved data changed notification mechanism
* Support connecting to custom SQLite builds instead of stock Android SQLite
* Enhanced code generation plugin API
* Add SquidRecyclerAdapter for working with RecyclerView (available in the `squidb-recyclerview` module)
* Other misc fixes and enhancements
* See [this wiki page](https://github.com/yahoo/squidb/wiki/Changes-in-SquiDB-2.0) for instructions on how to update from SquiDB 1.x to 2.0

Version 1.1.0 *(2015-07-29)*
----------------------------
 * Add the ability for generated models to implement interfaces using the @Implements annotation
 * Add the ability to pass information about failed database migrations to your AbstractDatabase hooks using MigrationFailedException.

 **Potentially incompatible change**: The protected onMigrationFailed method now takes as a single argument an instance of MigrationFailedException. If your AbstractDatabase subclass overrides this method, you will need to update it.
 * Fix a bug where creating indexes could fail on API < 16
 * Fix a bug where SQL that needed validating (e.g. from ContentProviderQueryBuilder) could get a false positive on API < 16
 * Fix bugs related to attaching databases when write-ahead logging is enabled
 * The Insert builder will no longer fail silently if you try to insert multiple sets of values on API < 16.
   It will instead throw an exception since the corresponding SQL is only valid on SQLite version 3.7.11 or higher,
   which was added in API 16.

Version 1.0.1 *(2015-06-18)*
----------------------------
 * Add a @PrimaryKey annotation. This enables two things that weren't possible before:
   * Declaring id columns not named "\_id"
   * Disabling the `AUTOINCREMENT` behavior with a flag in the @PrimaryKey annotation

Version 1.0.0 *(2015-05-29)*
----------------------------
 * Initial stable release of SquiDB
