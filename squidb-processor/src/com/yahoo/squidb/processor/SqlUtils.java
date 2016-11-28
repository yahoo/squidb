/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor;

import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.data.ModelSpec;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public final class SqlUtils {

    /**
     * Pattern for validating identifiers, based on SQLite's "simple" tokenizer. Identifier chars include:
     * <ul>
     * <li>All ASCII alphanumeric characters</li>
     * <li>The '_' character</li>
     * <li>Unicode codepoints between &#92;u00A1 and &#92;uFFFF</li>
     * </ul>
     */
    private static final Pattern IDENTIFIER = Pattern.compile("[\u00a1-\uffff\\p{Alnum}_]+");

    /**
     * Pattern for finding whitespace characters
     */
    private static final Pattern WHITESPACE = Pattern.compile("\\s");

    /**
     * Checks if the given word is a SQLite keyword. If a word is a SQLite keyword, it is possible that it could be
     * used as a table or column name, but it is highly recommended that you not do so. Use
     * {@link #isRestrictedKeyword(String)} to check if a word definitely cannot be used as an identifier.
     *
     * @return true if word is a SQLite keyword
     * @see #isRestrictedKeyword(String)
     */
    public static boolean isKeyword(String word) {
        return word == null || ALL_KEYWORDS.contains(word.toUpperCase());
    }

    /**
     * @return true if word is a restricted SQLite keyword, i.e. cannot be used as a table or column name.
     *
     * @see #isKeyword(String)
     */
    public static boolean isRestrictedKeyword(String word) {
        return word == null || RESTRICTED_KEYWORDS.contains(word.toUpperCase());
    }

    /**
     * Checks identifier (for e.g. a table or column name) against various constraints, including if the identifier
     * is a keyword or a restricted keyword, if the identifier is null or empty, and if the identifier starts with
     * or contains any characters that may cause trouble with SQLite/SquiDB.
     * <p>
     * If the identifier is known to not work well with SQLite/SquiDB, an error message with the reason will be logged.
     * If any suspicious characters are noticed but it is uncertain if the identifier will work or not, a warning will
     * be logged.
     *
     * @return false if the identifier definitely cannot be used with SQLite/SquiDB, true otherwise
     */
    public static boolean checkIdentifier(String identifier, String type, ModelSpec<?> modelSpec, Element element,
            AptUtils aptUtils) {
        String trimmedIdentifier = identifier == null ? null : identifier.trim();
        if (trimmedIdentifier != null && !trimmedIdentifier.equals(identifier)) {
            aptUtils.getMessager().printMessage(Diagnostic.Kind.WARNING, type + " name '" + identifier + "' has "
                    + "excess whitespace that should be trimmed", element);
        }
        identifier = trimmedIdentifier;
        
        if (AptUtils.isEmpty(identifier)) {
            modelSpec.logError(type + " name cannot be null or empty", element);
            return false;
        } else if (Character.isDigit(identifier.charAt(0)) || !IDENTIFIER.matcher(identifier.substring(0, 1))
                .matches()) {
            modelSpec.logError(type + " name '" + identifier + "' cannot start with " + identifier.charAt(0), element);
            return false;
        } else if (WHITESPACE.matcher(identifier).find()) {
            modelSpec.logError(type + " name '" + identifier + "' contains whitespace characters", element);
            return false;
        } else if (isKeyword(identifier)) {
            if (isRestrictedKeyword(identifier)) {
                modelSpec.logError(type + " name '" + identifier + "' is a reserved SQLite keyword that cannot be "
                        + "used as a " + type + " name", element);
                return false;
            } else {
                aptUtils.getMessager().printMessage(Diagnostic.Kind.WARNING, type + " name '" + identifier + "' is a "
                        + "SQLite keyword. It may be allowed as an identifier but it is recommended you choose a "
                        + "non-keyword name instead", element);
            }
        } else if (!IDENTIFIER.matcher(identifier).matches()) {
            // TODO: If we ever promote this to an error rather than a warning, many of the above cases can be consolidated
            aptUtils.getMessager().printMessage(Diagnostic.Kind.WARNING, type + " name '" + identifier + "' contains "
                    + "characters that may not be fully supported by SquiDB or SQLite in some cases. It is strongly "
                    + "recommended your identifiers only contain alphanumeric characters, underscores ('_'), and "
                    + "characters with codepoints \\u00A1-\\uFFFF. This may be considered an error in future versions "
                    + "of SquiDB.", element);
        }
        return true;
    }

    // This code is generated by running scripts/sqlite-keywords.sh. We should periodically check for new keywords,
    // add them to that script, and re-run it to update this code.
    private static final Set<String> ALL_KEYWORDS = new HashSet<>();
    private static final Set<String> RESTRICTED_KEYWORDS = new HashSet<>();
    static {
        ALL_KEYWORDS.add("ABORT");
        ALL_KEYWORDS.add("ACTION");
        ALL_KEYWORDS.add("ADD"); RESTRICTED_KEYWORDS.add("ADD");
        ALL_KEYWORDS.add("AFTER");
        ALL_KEYWORDS.add("ALL"); RESTRICTED_KEYWORDS.add("ALL");
        ALL_KEYWORDS.add("ALTER"); RESTRICTED_KEYWORDS.add("ALTER");
        ALL_KEYWORDS.add("ANALYZE");
        ALL_KEYWORDS.add("AND"); RESTRICTED_KEYWORDS.add("AND");
        ALL_KEYWORDS.add("AS"); RESTRICTED_KEYWORDS.add("AS");
        ALL_KEYWORDS.add("ASC");
        ALL_KEYWORDS.add("ATTACH");
        ALL_KEYWORDS.add("AUTOINCREMENT"); RESTRICTED_KEYWORDS.add("AUTOINCREMENT");
        ALL_KEYWORDS.add("BEFORE");
        ALL_KEYWORDS.add("BEGIN");
        ALL_KEYWORDS.add("BETWEEN"); RESTRICTED_KEYWORDS.add("BETWEEN");
        ALL_KEYWORDS.add("BY");
        ALL_KEYWORDS.add("CASCADE");
        ALL_KEYWORDS.add("CASE"); RESTRICTED_KEYWORDS.add("CASE");
        ALL_KEYWORDS.add("CAST");
        ALL_KEYWORDS.add("CHECK"); RESTRICTED_KEYWORDS.add("CHECK");
        ALL_KEYWORDS.add("COLLATE"); RESTRICTED_KEYWORDS.add("COLLATE");
        ALL_KEYWORDS.add("COLUMN");
        ALL_KEYWORDS.add("COMMIT"); RESTRICTED_KEYWORDS.add("COMMIT");
        ALL_KEYWORDS.add("CONFLICT");
        ALL_KEYWORDS.add("CONSTRAINT"); RESTRICTED_KEYWORDS.add("CONSTRAINT");
        ALL_KEYWORDS.add("CREATE"); RESTRICTED_KEYWORDS.add("CREATE");
        ALL_KEYWORDS.add("CROSS");
        ALL_KEYWORDS.add("CURRENT_DATE");
        ALL_KEYWORDS.add("CURRENT_TIME");
        ALL_KEYWORDS.add("CURRENT_TIMESTAMP");
        ALL_KEYWORDS.add("DATABASE");
        ALL_KEYWORDS.add("DEFAULT"); RESTRICTED_KEYWORDS.add("DEFAULT");
        ALL_KEYWORDS.add("DEFERRABLE"); RESTRICTED_KEYWORDS.add("DEFERRABLE");
        ALL_KEYWORDS.add("DEFERRED");
        ALL_KEYWORDS.add("DELETE"); RESTRICTED_KEYWORDS.add("DELETE");
        ALL_KEYWORDS.add("DESC");
        ALL_KEYWORDS.add("DETACH");
        ALL_KEYWORDS.add("DISTINCT"); RESTRICTED_KEYWORDS.add("DISTINCT");
        ALL_KEYWORDS.add("DROP"); RESTRICTED_KEYWORDS.add("DROP");
        ALL_KEYWORDS.add("EACH");
        ALL_KEYWORDS.add("ELSE"); RESTRICTED_KEYWORDS.add("ELSE");
        ALL_KEYWORDS.add("END");
        ALL_KEYWORDS.add("ESCAPE"); RESTRICTED_KEYWORDS.add("ESCAPE");
        ALL_KEYWORDS.add("EXCEPT"); RESTRICTED_KEYWORDS.add("EXCEPT");
        ALL_KEYWORDS.add("EXCLUSIVE");
        ALL_KEYWORDS.add("EXISTS"); RESTRICTED_KEYWORDS.add("EXISTS");
        ALL_KEYWORDS.add("EXPLAIN");
        ALL_KEYWORDS.add("FAIL");
        ALL_KEYWORDS.add("FOR");
        ALL_KEYWORDS.add("FOREIGN"); RESTRICTED_KEYWORDS.add("FOREIGN");
        ALL_KEYWORDS.add("FROM"); RESTRICTED_KEYWORDS.add("FROM");
        ALL_KEYWORDS.add("FULL");
        ALL_KEYWORDS.add("GLOB");
        ALL_KEYWORDS.add("GROUP"); RESTRICTED_KEYWORDS.add("GROUP");
        ALL_KEYWORDS.add("HAVING"); RESTRICTED_KEYWORDS.add("HAVING");
        ALL_KEYWORDS.add("IF"); RESTRICTED_KEYWORDS.add("IF");
        ALL_KEYWORDS.add("IGNORE");
        ALL_KEYWORDS.add("IMMEDIATE");
        ALL_KEYWORDS.add("IN"); RESTRICTED_KEYWORDS.add("IN");
        ALL_KEYWORDS.add("INDEX"); RESTRICTED_KEYWORDS.add("INDEX");
        ALL_KEYWORDS.add("INDEXED");
        ALL_KEYWORDS.add("INITIALLY");
        ALL_KEYWORDS.add("INNER");
        ALL_KEYWORDS.add("INSERT"); RESTRICTED_KEYWORDS.add("INSERT");
        ALL_KEYWORDS.add("INSTEAD");
        ALL_KEYWORDS.add("INTERSECT"); RESTRICTED_KEYWORDS.add("INTERSECT");
        ALL_KEYWORDS.add("INTO"); RESTRICTED_KEYWORDS.add("INTO");
        ALL_KEYWORDS.add("IS"); RESTRICTED_KEYWORDS.add("IS");
        ALL_KEYWORDS.add("ISNULL"); RESTRICTED_KEYWORDS.add("ISNULL");
        ALL_KEYWORDS.add("JOIN"); RESTRICTED_KEYWORDS.add("JOIN");
        ALL_KEYWORDS.add("KEY");
        ALL_KEYWORDS.add("LEFT");
        ALL_KEYWORDS.add("LIKE");
        ALL_KEYWORDS.add("LIMIT"); RESTRICTED_KEYWORDS.add("LIMIT");
        ALL_KEYWORDS.add("MATCH");
        ALL_KEYWORDS.add("NATURAL");
        ALL_KEYWORDS.add("NO");
        ALL_KEYWORDS.add("NOT"); RESTRICTED_KEYWORDS.add("NOT");
        ALL_KEYWORDS.add("NOTNULL"); RESTRICTED_KEYWORDS.add("NOTNULL");
        ALL_KEYWORDS.add("NULL"); RESTRICTED_KEYWORDS.add("NULL");
        ALL_KEYWORDS.add("OF");
        ALL_KEYWORDS.add("OFFSET");
        ALL_KEYWORDS.add("ON"); RESTRICTED_KEYWORDS.add("ON");
        ALL_KEYWORDS.add("OR"); RESTRICTED_KEYWORDS.add("OR");
        ALL_KEYWORDS.add("ORDER"); RESTRICTED_KEYWORDS.add("ORDER");
        ALL_KEYWORDS.add("OUTER");
        ALL_KEYWORDS.add("PLAN");
        ALL_KEYWORDS.add("PRAGMA");
        ALL_KEYWORDS.add("PRIMARY"); RESTRICTED_KEYWORDS.add("PRIMARY");
        ALL_KEYWORDS.add("QUERY");
        ALL_KEYWORDS.add("RAISE");
        ALL_KEYWORDS.add("RECURSIVE");
        ALL_KEYWORDS.add("REFERENCES"); RESTRICTED_KEYWORDS.add("REFERENCES");
        ALL_KEYWORDS.add("REGEXP");
        ALL_KEYWORDS.add("REINDEX");
        ALL_KEYWORDS.add("RELEASE");
        ALL_KEYWORDS.add("RENAME");
        ALL_KEYWORDS.add("REPLACE");
        ALL_KEYWORDS.add("RESTRICT");
        ALL_KEYWORDS.add("RIGHT");
        ALL_KEYWORDS.add("ROLLBACK");
        ALL_KEYWORDS.add("ROW");
        ALL_KEYWORDS.add("SAVEPOINT");
        ALL_KEYWORDS.add("SELECT"); RESTRICTED_KEYWORDS.add("SELECT");
        ALL_KEYWORDS.add("SET"); RESTRICTED_KEYWORDS.add("SET");
        ALL_KEYWORDS.add("TABLE"); RESTRICTED_KEYWORDS.add("TABLE");
        ALL_KEYWORDS.add("TEMP");
        ALL_KEYWORDS.add("TEMPORARY");
        ALL_KEYWORDS.add("THEN"); RESTRICTED_KEYWORDS.add("THEN");
        ALL_KEYWORDS.add("TO"); RESTRICTED_KEYWORDS.add("TO");
        ALL_KEYWORDS.add("TRANSACTION"); RESTRICTED_KEYWORDS.add("TRANSACTION");
        ALL_KEYWORDS.add("TRIGGER");
        ALL_KEYWORDS.add("UNION"); RESTRICTED_KEYWORDS.add("UNION");
        ALL_KEYWORDS.add("UNIQUE"); RESTRICTED_KEYWORDS.add("UNIQUE");
        ALL_KEYWORDS.add("UPDATE"); RESTRICTED_KEYWORDS.add("UPDATE");
        ALL_KEYWORDS.add("USING"); RESTRICTED_KEYWORDS.add("USING");
        ALL_KEYWORDS.add("VACUUM");
        ALL_KEYWORDS.add("VALUES"); RESTRICTED_KEYWORDS.add("VALUES");
        ALL_KEYWORDS.add("VIEW");
        ALL_KEYWORDS.add("VIRTUAL");
        ALL_KEYWORDS.add("WHEN"); RESTRICTED_KEYWORDS.add("WHEN");
        ALL_KEYWORDS.add("WHERE"); RESTRICTED_KEYWORDS.add("WHERE");
        ALL_KEYWORDS.add("WITH");
        ALL_KEYWORDS.add("WITHOUT");
    }

}
