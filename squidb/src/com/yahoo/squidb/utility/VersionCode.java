/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import com.yahoo.squidb.sql.SqlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a version code of the form {@code major.minor.micro.trailing}.
 */
public class VersionCode implements Comparable<VersionCode> {

    public static final VersionCode V3_7_4 = new VersionCode(3, 7, 4, 0); // Default on API 14, default minimum
    public static final VersionCode V3_7_11 = new VersionCode(3, 7, 11, 0); // Support for multi-row insert
    public static final VersionCode V3_8_3 = new VersionCode(3, 8, 3, 0); // Support for common table expressions
    public static final VersionCode LATEST = new VersionCode(3, 9, 2, 0); // Latest version

    private static final String VERSION_REGEX =
            "^([\\d]+)(?:\\.([\\d]+))?(?:\\.([\\d]+))?(?:\\.([\\d]+))?((?:[\\w\\-\\(\\)]+\\.)*[\\w\\-\\(\\)]+)?";
    private static Pattern pattern;

    private final int majorVersion;
    private final int minorVersion;
    private final int microVersion;
    private final int nanoVersion;
    private final String trailing;

    /**
     * @throws IllegalArgumentException if any value is less than zero.
     */
    public VersionCode(int major, int minor, int micro, int nano) {
        this(major, minor, micro, nano, null);
    }

    /**
     * @throws IllegalArgumentException if any value is less than zero.
     */
    public VersionCode(int major, int minor, int micro, int nano, String trailing) {
        if (major < 0 || minor < 0 || micro < 0 || nano < 0) {
            throw new IllegalArgumentException(
                    "Can't use a value less than zero to construct a VersionCode.");
        }
        majorVersion = major;
        minorVersion = minor;
        microVersion = micro;
        nanoVersion = nano;
        this.trailing = trailing;
    }

    /**
     * @return the major version number
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * @return the minor version number
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * @return the micro version number
     */
    public int getMicroVersion() {
        return microVersion;
    }

    /**
     * @return the nano version number
     */
    public int getNanoVersion() {
        return nanoVersion;
    }

    /**
     * @return the trailing text of the version code
     */
    public String getTrailingText() {
        return trailing;
    }

    /**
     * @return true if the version represented by this object is equal to or greater than the version represented by the
     * {@code version} argument.
     */
    public boolean isAtLeast(VersionCode version) {
        return this.compareTo(version) >= 0;
    }

    /**
     * @return true if the version represented by this object is equal to or greater than the version represented by the
     * {@code versionString} argument.
     */
    public boolean isAtLeast(String versionString) {
        return isAtLeast(parse(versionString));
    }

    /**
     * @return true if the version represented by this object is less than the version represented by the
     * {@code version} argument.
     */
    public boolean isLessThan(VersionCode version) {
        return this.compareTo(version) < 0;
    }

    /**
     * @return true if the version represented by this object is less than the version represented by the
     * {@code versionString} argument.
     */
    public boolean isLessThan(String versionString) {
        return isLessThan(parse(versionString));
    }

    /**
     * Parse a version string of the form {@code major.minor.micro.trailing}.
     * Major, minor, and micro must be numeric. Only the major version number is
     * required to successfully parse the string. If trailing alphanumeric
     * characters (up to the first non-word character) are present, a missing
     * minor or micro version is considered to be 0.
     *
     * <pre>
     * "1"              =&lt;  1.0.0
     * "1.2"            =&lt;  1.2.0
     * "1.2.3"          =&lt;  1.2.3
     * "1.2.3foo"       =&lt;  1.2.3foo
     * "1.2.3foo bar"   =&lt;  1.2.3foo
     * "1foo"           =&lt;  1.0.0foo
     * "1.2foo"         =&lt;  1.2.0foo
     * "foo"            =&lt;  0.0.0
     * </pre>
     *
     * @throws IllegalArgumentException if the input cannot be parsed.
     */
    public static VersionCode parse(String versionString) {
        if (SqlUtils.isEmpty(versionString)) {
            throw new IllegalArgumentException("Empty versionString");
        }

        if (pattern == null) {
            pattern = Pattern.compile(VERSION_REGEX);
        }

        Matcher matcher = pattern.matcher(versionString.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid versionString: " + versionString);
        }

        int major, minor, micro, nano;
        // group(0) is the full match, so start at 1
        // regex guarantees group(1) is not null
        String majorString = matcher.group(1);
        major = Integer.parseInt(majorString);
        String minorString = matcher.group(2);
        minor = minorString == null ? 0 : Integer.parseInt(minorString);
        String microString = matcher.group(3);
        micro = microString == null ? 0 : Integer.parseInt(microString);
        String nanoString = matcher.group(4);
        nano = nanoString == null ? 0 : Integer.parseInt(nanoString);
        String trailing = matcher.group(5);

        return new VersionCode(major, minor, micro, nano, trailing);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VersionCode)) {
            return false;
        }

        VersionCode other = (VersionCode) o;
        return this.compareTo(other) == 0;
    }

    @Override
    public int compareTo(VersionCode other) {
        if (this == other) {
            return 0;
        }
        int result = this.majorVersion - other.majorVersion;
        if (result != 0) {
            return result;
        }
        result = this.minorVersion - other.minorVersion;
        if (result != 0) {
            return result;
        }
        result = this.microVersion - other.microVersion;
        if (result != 0) {
            return result;
        }
        result = this.nanoVersion - other.nanoVersion;
        if (result != 0) {
            return result;
        }

        if (this.trailing == null) {
            return other.trailing == null ? 0 : -1;
        }

        return other.trailing == null ? 1
                : this.trailing.compareTo(other.trailing);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(Integer.toString(majorVersion))
                .append('.').append(Integer.toString(minorVersion))
                .append('.').append(Integer.toString(microVersion));
        if (nanoVersion > 0) {
            builder.append('.').append(nanoVersion);
        }
        if (!SqlUtils.isEmpty(trailing)) {
            builder.append(trailing);
        }

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hash = majorVersion;
        hash = hash * 31 + minorVersion;
        hash = hash * 31 + microVersion;
        hash = hash * 31 + nanoVersion;
        hash = hash * 31 + (trailing == null ? 0 : trailing.hashCode());
        return hash;
    }
}
