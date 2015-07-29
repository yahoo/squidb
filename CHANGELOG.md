Change Log
==========

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
   * Declaring id columns not named "_id"
   * Disabling the `AUTOINCREMENT` behavior with a flag in the @PrimaryKey annotation

Version 1.0.0 *(2015-05-29)*
----------------------------

 * Initial stable release of SquiDB
