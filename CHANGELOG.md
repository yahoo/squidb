Change Log
==========

Version 3.2.3 *(2016-12-13)*
* Fix a bug in model `clone()` which would cause the cloned instance to be sharing a transitory values map with the original model object
* Fix some bugs in the `squidb-json` addon that could cause JSON property getters to return incorrect values under certain conditions
* Fix transitory values to be able to handle null values and keys correctly

Version 3.2.2 *(2016-12-05)*
----------------------------
* Introduce a new error logging mechanism in the code generator that allows SquiDB to log errors without generating a bunch of "cannot find symbol \[ModelClass\]" errors
* Implement validation in the code generator to guard against illegal column or table names, e.g. those that use SQLite keywords or contain invalid characters.

Version 3.2.1 *(2016-11-07)*
----------------------------
* Fix a bug that could cause `containsNonNullValue(Property<?>)` to return an incorrect result under certain conditions.
* Eliminate several harmless warnings that could be emitted by the code generator when compiling using Java 1.8.
* Deprecate the SQLite version constants in the `VersionCode` class in favor of use-case specific constants in other classes. Most notably, `VersionCode.LATEST` is superseded by `SQLiteBindingsAdapter.SQLITE_VERSION` for the `squidb-sqlite-bindings` project, and `VersionCode.V3_7_11` is superseded by `Insert.SQLITE_VERSION_MULTI_ROW_INSERT`.
* Minor enhancements to null argument handling in the `in()/notIn()` criterion creating methods.

Version 3.2.0 *(2016-11-01)*
----------------------------
* Add an experimental, under-the-hood change that can lead to performance increases of up to 70% for transactions inserting a large number of rows, and ~25-50% in the average case when inserting rows. The change is disabled by default; to enable it, users can call `setPreparedInsertCacheEnabled(true)` in the `onConfigure` method of their SquidDatabase. See the javadocs of `setPreparedInsertCacheEnabled` for more information.
* Add a new `prepareStatement` API to SquidDatabase, which allows users to prepare and reuse low-level (non-query) SQLite statements. Reusing these prepared statements can result in a performance improvement when used judiciously.
* Introduce the [`ArgumentResolver`](https://github.com/yahoo/squidb/blob/master/squidb/src/com/yahoo/squidb/sql/ArgumentResolver.java) API that lets users control how non-primitive values are bound to SQL statements (e.g., a user could override enum value handling to bind the values using `ordinal()` instead of `name()`). Custom `ArgumentResolver` implementations can be used in a SquidDatabase using the extensible `buildCompileContext` method.
* Move the `copyDatabase()` debugging utility into SquidDatabase. Fix some bugs with its implementation to make it behave nicely with databases in WAL mode and be thread safe.
* Some minor enhancements to the BasicPropertyGenerator plugin API that make it easier to modify the declarations of a model's getters and setters.
* Bump the SQLite version in `squidb-sqlite-bindings` to 3.15.0.

Version 3.1.3 *(2016-09-02)*
----------------------------
* Fix a bug where enums that override toString() would be incorrectly serialized to the database. It's likely that you would have noticed a problem before now if this happened to you, but if you've been persisting an enum column with an enum that overrides toString(), you may have to run a data migration to fix the issue. Sorry to anyone affected by this!

Version 3.1.2 *(2016-08-24)*
----------------------------
* Fix a bug introduced in version 3.1.1 where automatically generating the deprecated default ID property would cause a compilation error. This issue only affected users who have not yet updated their model specs as described in the 3.1.0 release notes.

Version 3.1.1 *(2016-08-23)*
----------------------------
* **Edit:** A bug with the deprecated default `INTEGER PRIMARY KEY` column existed in this version. Users should use 3.1.2 instead.
* Further enhancements to the `INTEGER PRIMARY KEY` changes introduced in version 3.1.0. Any columns acting as an alias to the model rowid will now generate named getters and setters, which will delegate to `getRowId()/setRowId()` under the hood. This makes rowid alias columns easier to work with.
* Enhance code generation plugins by allowing them to declare compile-time options that they consume using the `@SupportedOptions` annotation. This makes it easier for the code generator to warn about unused/mistyped options passed to SquiDB as well as allowing user-defined plugins to consume their own custom option flags.

Version 3.1.0 *(2016-07-27)*
----------------------------
* Removed restrictions on the `@PrimaryKey` annotation so that columns of any type may be declared as the primary key for the table. SquiDB will stop generating the `ID` property as a rowid alias in a future version; as such the `getId()` and `setId()` methods in all `TableModel` subclasses are deprecated in favor of the more descriptively named `getRowId()` and `setRowId()`. See [this wiki page](https://github.com/yahoo/squidb/wiki/Primary-keys) for information on how to future-proof your existing model specs for this change.
* SquidCursor now exposes the "hint" class object of its type argument via the `getModelHintClass()` method

Version 3.0.0 *(2016-06-23)*
----------------------------
* Version 3.0 adds cross-platform support by supporting compiling SquiDB with [Google's j2objc tool](http://j2objc.org/). The new `squidb-android` and `squidb-ios` modules provide low-level platform-specific SQLite access, while users interact only with the higher-level SquidDatabase and SQL builder APIs. Write your database code and other business logic in Java, and compile it to run on both Android and iOS devices. See [this wiki page](https://github.com/yahoo/squidb/wiki/Changes-in-SquiDB-3.0) for instructions on how to update to the new version.
* Introduce the new `squidb-json` addon, which facilitates serializing arbitrary objects to String columns as JSON and working with the new [SQLite json1 extension](http://sqlite.org/json1.html). See [this wiki page](https://github.com/yahoo/squidb/wiki/JSON-support-in-SquiDB) for documentation about what you can do with this addon.

Version 2.1.0 *(2016-06-14)*
----------------------------
* The `ViewModel#mapToModel` API would break down if the ViewModel consisted of multiple joins on the same table with different aliases.
  This release adds a new version of the `ViewModel#mapToModel` API that takes a second table alias argument to specify which table alias
  should be used when mapping result columns back to their source table rows.
* Generated models now contain a Javadoc link back to their model specs for easier navigation in the IDE
* Fix several harmless warnings that could be emitted by the code generator
* Bump the SQLite version in the `squidb-sqlite-bindings` project to 3.13.0
* SquiDB 3.0 will be arriving soon, adding support for iOS via [Google's j2objc tool](http://j2objc.org/). Development is in progress on [this branch](https://github.com/yahoo/squidb/tree/dev_3.0).

Version 2.0.3 *(2016-04-15)*
----------------------------
* Add the ability to use enum types in column definitions. Enums properties serialize their values to strings for storage
* Significantly improved some internal locking code so that it is safe to call `close()`/`clear()`/`recreate()` on a SquidDatabase from any thread. These methods will now block if any transactions are ongoing to ensure that the database cannot be closed from one thread while another is writing to it
* Add an experimental hook `onDatabaseOpenFailed()` that allows users to attempt to recover if the database could not be opened for a reason other than a failed migration. This hook is considered beta and experimental, so it may change in the future
* Fix a bug in the code generator that would generate column definitions from `private static final` fields in model specs
* Fix a bug where data changed notifiers could be triggered during database open if database writes occurred during any of the open or upgrade hooks
* Bump the SQLite version in the `squidb-sqlite-bindings` project to 3.12.1
* Deprecated a few methods in SquidDatabase that will be removed in SquiDB 3.0

Version 2.0.2 *(2015-12-10)*
----------------------------
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
