/*
 * Microsoft JDBC Driver for SQL Server Copyright(c) Microsoft Corporation All rights reserved. This program is made
 * available under the terms of the MIT License. See the LICENSE file in the project root for more information.
 */

package com.microsoft.sqlserver.jdbc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import com.microsoft.sqlserver.jdbc.dns.DNSKerberosLocator;


/**
 * Various driver utilities.
 *
 */
final class Util {
    final static String SYSTEM_SPEC_VERSION = System.getProperty("java.specification.version");
    final static char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    final static String WSIDNotAvailable = ""; // default string when WSID is not available

    final static String ACTIVITY_ID_TRACE_PROPERTY = "com.microsoft.sqlserver.jdbc.traceactivity";

    // The JRE is identified by the string below so that the driver can make
    // any vendor or version specific decisions
    static final String SYSTEM_JRE = System.getProperty("java.vendor") + " " + System.getProperty("java.version");

    static boolean isIBM() {
        return SYSTEM_JRE.startsWith("IBM");
    }

    static final Boolean isCharType(int jdbcType) {
        switch (jdbcType) {
            case java.sql.Types.CHAR:
            case java.sql.Types.NCHAR:
            case java.sql.Types.VARCHAR:
            case java.sql.Types.NVARCHAR:
            case java.sql.Types.LONGVARCHAR:
            case java.sql.Types.LONGNVARCHAR:
                return true;
            default:
                return false;
        }
    }

    static final Boolean isCharType(SSType ssType) {
        switch (ssType) {
            case CHAR:
            case NCHAR:
            case VARCHAR:
            case NVARCHAR:
            case VARCHARMAX:
            case NVARCHARMAX:
                return true;
            default:
                return false;
        }
    }

    static final Boolean isBinaryType(SSType ssType) {
        switch (ssType) {
            case BINARY:
            case VARBINARY:
            case VARBINARYMAX:
            case IMAGE:
                return true;
            default:
                return false;
        }
    }

    static final Boolean isBinaryType(int jdbcType) {
        switch (jdbcType) {
            case java.sql.Types.BINARY:
            case java.sql.Types.VARBINARY:
            case java.sql.Types.LONGVARBINARY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Read a short int from a byte stream
     * 
     * @param data
     *        the databytes
     * @param nOffset
     *        offset to read from
     * @return the value
     */
    /* L0 */ static short readShort(byte data[], int nOffset) {
        return (short) ((data[nOffset] & 0xff) | ((data[nOffset + 1] & 0xff) << 8));
    }

    /**
     * Read an unsigned short int (16 bits) from a byte stream
     * 
     * @param data
     *        the databytes
     * @param nOffset
     *        offset to read from
     * @return the value
     */
    /* L0 */ static int readUnsignedShort(byte data[], int nOffset) {
        return ((data[nOffset] & 0xff) | ((data[nOffset + 1] & 0xff) << 8));
    }

    static int readUnsignedShortBigEndian(byte data[], int nOffset) {
        return ((data[nOffset] & 0xFF) << 8) | (data[nOffset + 1] & 0xFF);
    }

    static void writeShort(short value, byte valueBytes[], int offset) {
        valueBytes[offset + 0] = (byte) ((value >> 0) & 0xFF);
        valueBytes[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    static void writeShortBigEndian(short value, byte valueBytes[], int offset) {
        valueBytes[offset + 0] = (byte) ((value >> 8) & 0xFF);
        valueBytes[offset + 1] = (byte) ((value >> 0) & 0xFF);
    }

    /**
     * Read an int from a byte stream
     * 
     * @param data
     *        the databytes
     * @param nOffset
     *        offset to read from
     * @return the value
     */
    /* L0 */ static int readInt(byte data[], int nOffset) {
        int b1 = ((int) data[nOffset + 0] & 0xff);
        int b2 = ((int) data[nOffset + 1] & 0xff) << 8;
        int b3 = ((int) data[nOffset + 2] & 0xff) << 16;
        int b4 = ((int) data[nOffset + 3] & 0xff) << 24;
        return b4 | b3 | b2 | b1;
    }

    static int readIntBigEndian(byte data[], int nOffset) {
        return ((data[nOffset + 3] & 0xFF) << 0) | ((data[nOffset + 2] & 0xFF) << 8)
                | ((data[nOffset + 1] & 0xFF) << 16) | ((data[nOffset + 0] & 0xFF) << 24);
    }

    static void writeInt(int value, byte valueBytes[], int offset) {
        valueBytes[offset + 0] = (byte) ((value >> 0) & 0xFF);
        valueBytes[offset + 1] = (byte) ((value >> 8) & 0xFF);
        valueBytes[offset + 2] = (byte) ((value >> 16) & 0xFF);
        valueBytes[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    static void writeIntBigEndian(int value, byte valueBytes[], int offset) {
        valueBytes[offset + 0] = (byte) ((value >> 24) & 0xFF);
        valueBytes[offset + 1] = (byte) ((value >> 16) & 0xFF);
        valueBytes[offset + 2] = (byte) ((value >> 8) & 0xFF);
        valueBytes[offset + 3] = (byte) ((value >> 0) & 0xFF);
    }

    static void writeLongBigEndian(long value, byte valueBytes[], int offset) {
        valueBytes[offset + 0] = (byte) ((value >> 56) & 0xFF);
        valueBytes[offset + 1] = (byte) ((value >> 48) & 0xFF);
        valueBytes[offset + 2] = (byte) ((value >> 40) & 0xFF);
        valueBytes[offset + 3] = (byte) ((value >> 32) & 0xFF);
        valueBytes[offset + 4] = (byte) ((value >> 24) & 0xFF);
        valueBytes[offset + 5] = (byte) ((value >> 16) & 0xFF);
        valueBytes[offset + 6] = (byte) ((value >> 8) & 0xFF);
        valueBytes[offset + 7] = (byte) ((value >> 0) & 0xFF);
    }

    static BigDecimal readBigDecimal(byte valueBytes[], int valueLength, int scale) {
        int sign = (0 == valueBytes[0]) ? -1 : 1;
        byte[] magnitude = new byte[valueLength - 1];
        for (int i = 1; i <= magnitude.length; i++)
            magnitude[magnitude.length - i] = valueBytes[i];
        return new BigDecimal(new BigInteger(sign, magnitude), scale);
    }

    /**
     * Reads a long value from byte array.
     * 
     * @param data
     *        the byte array.
     * @param nOffset
     *        the offset into byte array to start reading.
     * @return long value as read from bytes.
     */
    /* L0 */static long readLong(byte data[], int nOffset) {
        return ((long) (data[nOffset + 7] & 0xff) << 56) | ((long) (data[nOffset + 6] & 0xff) << 48)
                | ((long) (data[nOffset + 5] & 0xff) << 40) | ((long) (data[nOffset + 4] & 0xff) << 32)
                | ((long) (data[nOffset + 3] & 0xff) << 24) | ((long) (data[nOffset + 2] & 0xff) << 16)
                | ((long) (data[nOffset + 1] & 0xff) << 8) | ((long) (data[nOffset] & 0xff));
    }

    /**
     * Writes a long to byte array.
     *
     * @param value
     *        long value to write.
     * @param valueBytes
     *        the byte array.
     * @param offset
     *        the offset inside byte array.
     */
    static void writeLong(long value, byte valueBytes[], int offset) {
        valueBytes[offset++] = (byte) ((value) & 0xFF);
        valueBytes[offset++] = (byte) ((value >> 8) & 0xFF);
        valueBytes[offset++] = (byte) ((value >> 16) & 0xFF);
        valueBytes[offset++] = (byte) ((value >> 24) & 0xFF);
        valueBytes[offset++] = (byte) ((value >> 32) & 0xFF);
        valueBytes[offset++] = (byte) ((value >> 40) & 0xFF);
        valueBytes[offset++] = (byte) ((value >> 48) & 0xFF);
        valueBytes[offset] = (byte) ((value >> 56) & 0xFF);
    }

    /**
     * Parse a JDBC URL into a set of properties.
     * 
     * @param url
     *        the JDBC URL
     * @param logger
     * @return the properties
     * @throws SQLServerException
     */
    /* L0 */ static Properties parseUrl(String url, Logger logger) throws SQLServerException {
        Properties p = new Properties();
        String tmpUrl = url;
        String sPrefix = "jdbc:sqlserver://";
        String result = "";
        String name = "";
        String value = "";
        StringBuilder builder;

        if (!tmpUrl.startsWith(sPrefix))
            return null;

        tmpUrl = tmpUrl.substring(sPrefix.length());
        int i;

        // Simple finite state machine.
        // always look at one char at a time
        final int inStart = 0;
        final int inServerName = 1;
        final int inPort = 2;
        final int inInstanceName = 3;
        final int inEscapedValueStart = 4;
        final int inEscapedValueEnd = 5;
        final int inValue = 6;
        final int inName = 7;

        int state = inStart;
        char ch;
        i = 0;
        while (i < tmpUrl.length()) {
            ch = tmpUrl.charAt(i);
            switch (state) {
                case inStart: {
                    if (ch == ';') {
                        // done immediately
                        state = inName;
                    } else {
                        builder = new StringBuilder();
                        builder.append(result);
                        builder.append(ch);
                        result = builder.toString();
                        state = inServerName;
                    }
                    break;
                }

                case inServerName: {
                    if (ch == ';' || ch == ':' || ch == '\\') {
                        // non escaped trim the string
                        result = result.trim();
                        if (result.length() > 0) {
                            p.put(SQLServerDriverStringProperty.SERVER_NAME.toString(), result);
                            if (logger.isLoggable(Level.FINE)) {
                                logger.fine("Property:serverName " + "Value:" + result);
                            }
                        }
                        result = "";

                        if (ch == ';')
                            state = inName;
                        else if (ch == ':')
                            state = inPort;
                        else
                            state = inInstanceName;
                    } else {
                        builder = new StringBuilder();
                        builder.append(result);
                        builder.append(ch);
                        result = builder.toString();
                        // same state
                    }
                    break;
                }

                case inPort: {
                    if (ch == ';') {
                        result = result.trim();
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Property:portNumber " + "Value:" + result);
                        }
                        p.put(SQLServerDriverIntProperty.PORT_NUMBER.toString(), result);
                        result = "";
                        state = inName;
                    } else {
                        builder = new StringBuilder();
                        builder.append(result);
                        builder.append(ch);
                        result = builder.toString();
                        // same state
                    }
                    break;
                }
                case inInstanceName: {
                    if (ch == ';' || ch == ':') {
                        // non escaped trim the string
                        result = result.trim();
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Property:instanceName " + "Value:" + result);
                        }
                        p.put(SQLServerDriverStringProperty.INSTANCE_NAME.toString(), result.toLowerCase(Locale.US));
                        result = "";

                        if (ch == ';')
                            state = inName;
                        else
                            state = inPort;
                    } else {
                        builder = new StringBuilder();
                        builder.append(result);
                        builder.append(ch);
                        result = builder.toString();
                        // same state
                    }
                    break;
                }
                case inName: {
                    if (ch == '=') {
                        // name is never escaped!
                        name = name.trim();
                        if (name.length() <= 0) {
                            SQLServerException.makeFromDriverError(null, null,
                                    SQLServerException.getErrString("R_errorConnectionString"), null, true);
                        }
                        state = inValue;
                    } else if (ch == ';') {
                        name = name.trim();
                        if (name.length() > 0) {
                            SQLServerException.makeFromDriverError(null, null,
                                    SQLServerException.getErrString("R_errorConnectionString"), null, true);
                        }
                        // same state
                    } else {
                        builder = new StringBuilder();
                        builder.append(name);
                        builder.append(ch);
                        name = builder.toString();
                        // same state
                    }
                    break;
                }
                case inValue: {
                    if (ch == ';') {
                        // simple value trim
                        value = value.trim();
                        name = SQLServerDriver.getNormalizedPropertyName(name, logger);
                        if (null != name) {
                            if (logger.isLoggable(Level.FINE)) {
                                if (!name.equals(SQLServerDriverStringProperty.USER.toString())) {
                                    if (!name.toLowerCase(Locale.ENGLISH).contains("password")
                                            && !name.toLowerCase(Locale.ENGLISH).contains("keystoresecret")) {
                                        logger.fine("Property:" + name + " Value:" + value);
                                    } else {
                                        logger.fine("Property:" + name);
                                    }
                                }
                            }
                            p.put(name, value);
                        }
                        name = "";
                        value = "";
                        state = inName;

                    } else if (ch == '{') {
                        state = inEscapedValueStart;
                        value = value.trim();
                        if (value.length() > 0) {
                            SQLServerException.makeFromDriverError(null, null,
                                    SQLServerException.getErrString("R_errorConnectionString"), null, true);
                        }
                    } else {
                        builder = new StringBuilder();
                        builder.append(value);
                        builder.append(ch);
                        value = builder.toString();
                        // same state
                    }
                    break;
                }
                case inEscapedValueStart: {
                    if (ch == '}') {
                        // no trimming use the value as it is.
                        name = SQLServerDriver.getNormalizedPropertyName(name, logger);
                        if (null != name) {
                            if (logger.isLoggable(Level.FINE)) {
                                if (!name.equals(SQLServerDriverStringProperty.USER.toString())
                                        && !name.equals(SQLServerDriverStringProperty.PASSWORD.toString()))
                                    logger.fine("Property:" + name + " Value:" + value);
                            }
                            p.put(name, value);
                        }

                        name = "";
                        value = "";
                        // to eat the spaces until the ; potentially we could do without the state but
                        // it would not be clean
                        state = inEscapedValueEnd;
                    } else {
                        builder = new StringBuilder();
                        builder.append(value);
                        builder.append(ch);
                        value = builder.toString();
                        // same state
                    }
                    break;
                }
                case inEscapedValueEnd: {
                    if (ch == ';') // eat space chars till ; anything else is an error
                    {
                        state = inName;
                    } else if (ch != ' ') {
                        // error if the chars are not space
                        SQLServerException.makeFromDriverError(null, null,
                                SQLServerException.getErrString("R_errorConnectionString"), null, true);
                    }
                    break;
                }

                default:
                    assert false : "parseURL: Invalid state " + state;
            }
            i++;
        }

        // Exit
        switch (state) {
            case inServerName:
                result = result.trim();
                if (result.length() > 0) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Property:serverName " + "Value:" + result);
                    }
                    p.put(SQLServerDriverStringProperty.SERVER_NAME.toString(), result);
                }
                break;
            case inPort:
                result = result.trim();
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Property:portNumber " + "Value:" + result);
                }
                p.put(SQLServerDriverIntProperty.PORT_NUMBER.toString(), result);
                break;
            case inInstanceName:
                result = result.trim();
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Property:instanceName " + "Value:" + result);
                }
                p.put(SQLServerDriverStringProperty.INSTANCE_NAME.toString(), result);
                break;
            case inValue:
                // simple value trim
                value = value.trim();
                name = SQLServerDriver.getNormalizedPropertyName(name, logger);
                if (null != name) {
                    if (logger.isLoggable(Level.FINE)) {
                        if (!name.equals(SQLServerDriverStringProperty.USER.toString())
                                && !name.equals(SQLServerDriverStringProperty.PASSWORD.toString())
                                && !name.equals(SQLServerDriverStringProperty.KEY_STORE_SECRET.toString()))
                            logger.fine("Property:" + name + " Value:" + value);
                    }
                    p.put(name, value);
                }

                break;
            case inEscapedValueEnd:
            case inStart:
                // do nothing!
                break;
            case inName: {
                name = name.trim();
                if (name.length() > 0) {
                    SQLServerException.makeFromDriverError(null, null,
                            SQLServerException.getErrString("R_errorConnectionString"), null, true);
                }

                break;
            }
            default:
                SQLServerException.makeFromDriverError(null, null,
                        SQLServerException.getErrString("R_errorConnectionString"), null, true);
        }
        return p;
    }

    /**
     * Accepts a SQL identifier (such as a column name or table name) and escapes the identifier using SQL Server
     * bracket escaping rules. Assumes that the incoming identifier is unescaped.
     * 
     * @inID input identifier to escape.
     * @return the escaped value.
     */
    static String escapeSQLId(String inID) {
        // SQL bracket escaping rules.
        // Given <identifier> yields -> [<identifier>]
        // Where <identifier> is first escaped to replace all
        // instances of "]" with "]]".
        // For example, column name "abc" -> "[abc]"
        // For example, column name "]" -> "[]]]"
        // For example, column name "]ab]cd" -> "[]]ab]]cd]"
        char ch;

        // Add 2 extra chars for open and closing brackets.
        StringBuilder outID = new StringBuilder(inID.length() + 2);

        outID.append('[');
        for (int i = 0; i < inID.length(); i++) {
            ch = inID.charAt(i);
            if (']' == ch)
                outID.append("]]");
            else
                outID.append(ch);
        }
        outID.append(']');
        return outID.toString();
    }

    /**
     * Checks if duplicate columns exists, in O(n) time.
     * 
     * @param columnName
     *        the name of the column
     * @throws SQLServerException
     *         when a duplicate column exists
     */
    static void checkDuplicateColumnName(String columnName, Set<String> columnNames) throws SQLServerException {
        // columnList.add will return false if the same column name already exists
        if (!columnNames.add(columnName)) {
            MessageFormat form = new MessageFormat(SQLServerException.getErrString("R_TVPDuplicateColumnName"));
            Object[] msgArgs = {columnName};
            throw new SQLServerException(null, form.format(msgArgs), null, 0, false);
        }
    }

    /**
     * Reads a UNICODE string from byte buffer at offset (up to byteLength).
     * 
     * @param b
     *        the buffer containing UNICODE bytes.
     * @param offset
     *        - the offset into b where the UNICODE string starts.
     * @param byteLength
     *        - the length in bytes of the UNICODE string.
     * @param conn
     *        - the SQLServerConnection object.
     * @return new String with UNICODE data inside.
     */
    static String readUnicodeString(byte[] b, int offset, int byteLength,
            SQLServerConnection conn) throws SQLServerException {
        try {
            return new String(b, offset, byteLength, Encoding.UNICODE.charset());
        } catch (IndexOutOfBoundsException ex) {
            String txtMsg = SQLServerException
                    .checkAndAppendClientConnId(SQLServerException.getErrString("R_stringReadError"), conn);
            MessageFormat form = new MessageFormat(txtMsg);
            Object[] msgArgs = {offset};
            // Re-throw SQLServerException if conversion fails.
            throw new SQLServerException(form.format(msgArgs), null, 0, ex);
        }

    }

    // NOTE: This is for display purposes ONLY. NOT TO BE USED for data conversion.
    /**
     * Converts byte array to a string representation of hex bytes for display purposes.
     * 
     * @param b
     *        the source buffer.
     * @return "hexized" string representation of bytes.
     */
    static String byteToHexDisplayString(byte[] b) {
        if (null == b)
            return "(null)";
        int hexVal;
        StringBuilder sb = new StringBuilder(b.length * 2 + 2);
        sb.append("0x");
        for (byte aB : b) {
            hexVal = aB & 0xFF;
            sb.append(hexChars[(hexVal & 0xF0) >> 4]);
            sb.append(hexChars[(hexVal & 0x0F)]);
        }
        return sb.toString();
    }

    /**
     * Converts byte array to a string representation of hex bytes.
     * 
     * @param b
     *        the source buffer.
     * @return "hexized" string representation of bytes.
     */
    static String bytesToHexString(byte[] b, int length) {
        StringBuilder sb = new StringBuilder(length * 2);
        for (int i = 0; i < length; i++) {
            int hexVal = b[i] & 0xFF;
            sb.append(hexChars[(hexVal & 0xF0) >> 4]);
            sb.append(hexChars[(hexVal & 0x0F)]);
        }
        return sb.toString();
    }

    /**
     * Looks up local hostname of client machine.
     * 
     * @exception UnknownHostException
     *            if local hostname is not found.
     * @return hostname string or ip of host if hostname cannot be resolved. If neither hostname or ip found returns ""
     *         per spec.
     */
    static String lookupHostName() {

        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            if (null != localAddress) {
                String value = localAddress.getHostName();
                if (null != value && value.length() > 0)
                    return value;

                value = localAddress.getHostAddress();
                if (null != value && value.length() > 0)
                    return value;
            }
        } catch (UnknownHostException e) {
            return WSIDNotAvailable;
        }
        // If hostname not found, return standard "" string.
        return WSIDNotAvailable;
    }

    static final byte[] asGuidByteArray(UUID aId) {
        long msb = aId.getMostSignificantBits();
        long lsb = aId.getLeastSignificantBits();
        byte[] buffer = new byte[16];
        Util.writeLongBigEndian(msb, buffer, 0);
        Util.writeLongBigEndian(lsb, buffer, 8);

        // For the first three fields, UUID uses network byte order,
        // Guid uses native byte order. So we need to reverse
        // the first three fields before sending to server.

        byte tmpByte;

        // Reverse the first 4 bytes
        tmpByte = buffer[0];
        buffer[0] = buffer[3];
        buffer[3] = tmpByte;
        tmpByte = buffer[1];
        buffer[1] = buffer[2];
        buffer[2] = tmpByte;

        // Reverse the 5th and the 6th
        tmpByte = buffer[4];
        buffer[4] = buffer[5];
        buffer[5] = tmpByte;

        // Reverse the 7th and the 8th
        tmpByte = buffer[6];
        buffer[6] = buffer[7];
        buffer[7] = tmpByte;

        return buffer;
    }

    static final UUID readGUIDtoUUID(byte[] inputGUID) throws SQLServerException {
        if (inputGUID.length != 16) {
            throw new SQLServerException("guid length must be 16", null);
        }

        // For the first three fields, UUID uses network byte order,
        // Guid uses native byte order. So we need to reverse
        // the first three fields before creating a UUID.

        byte tmpByte;

        // Reverse the first 4 bytes
        tmpByte = inputGUID[0];
        inputGUID[0] = inputGUID[3];
        inputGUID[3] = tmpByte;
        tmpByte = inputGUID[1];
        inputGUID[1] = inputGUID[2];
        inputGUID[2] = tmpByte;

        // Reverse the 5th and the 6th
        tmpByte = inputGUID[4];
        inputGUID[4] = inputGUID[5];
        inputGUID[5] = tmpByte;

        // Reverse the 7th and the 8th
        tmpByte = inputGUID[6];
        inputGUID[6] = inputGUID[7];
        inputGUID[7] = tmpByte;

        long msb = 0L;
        for (int i = 0; i < 8; i++) {
            msb = msb << 8 | ((long) inputGUID[i] & 0xFFL);
        }
        long lsb = 0L;
        for (int i = 8; i < 16; i++) {
            lsb = lsb << 8 | ((long) inputGUID[i] & 0xFFL);
        }
        return new UUID(msb, lsb);
    }

    static final String readGUID(byte[] inputGUID) throws SQLServerException {
        String guidTemplate = "NNNNNNNN-NNNN-NNNN-NNNN-NNNNNNNNNNNN";
        byte guid[] = inputGUID;

        StringBuilder sb = new StringBuilder(guidTemplate.length());
        for (int i = 0; i < 4; i++) {
            sb.append(Util.hexChars[(guid[3 - i] & 0xF0) >> 4]);
            sb.append(Util.hexChars[guid[3 - i] & 0x0F]);
        }
        sb.append('-');
        for (int i = 0; i < 2; i++) {
            sb.append(Util.hexChars[(guid[5 - i] & 0xF0) >> 4]);
            sb.append(Util.hexChars[guid[5 - i] & 0x0F]);
        }
        sb.append('-');
        for (int i = 0; i < 2; i++) {
            sb.append(Util.hexChars[(guid[7 - i] & 0xF0) >> 4]);
            sb.append(Util.hexChars[guid[7 - i] & 0x0F]);
        }
        sb.append('-');
        for (int i = 0; i < 2; i++) {
            sb.append(Util.hexChars[(guid[8 + i] & 0xF0) >> 4]);
            sb.append(Util.hexChars[guid[8 + i] & 0x0F]);
        }
        sb.append('-');
        for (int i = 0; i < 6; i++) {
            sb.append(Util.hexChars[(guid[10 + i] & 0xF0) >> 4]);
            sb.append(Util.hexChars[guid[10 + i] & 0x0F]);
        }

        return sb.toString();
    }

    static boolean isActivityTraceOn() {
        LogManager lm = LogManager.getLogManager();
        String activityTrace = lm.getProperty(ACTIVITY_ID_TRACE_PROPERTY);
        return ("on".equalsIgnoreCase(activityTrace));
    }

    /**
     * Determines if a column value should be transparently decrypted (based on SQLServerStatement and the connection
     * string settings).
     * 
     * @return true if the value should be transparently decrypted, false otherwise.
     */
    static boolean shouldHonorAEForRead(SQLServerStatementColumnEncryptionSetting stmtColumnEncryptionSetting,
            SQLServerConnection connection) {
        // Command leve setting trumps all
        switch (stmtColumnEncryptionSetting) {
            case Disabled:
                return false;
            case Enabled:
            case ResultSetOnly:
                return true;
            default:
                // Check connection level setting!
                assert SQLServerStatementColumnEncryptionSetting.UseConnectionSetting == stmtColumnEncryptionSetting : "Unexpected value for command level override";
                return (connection != null && connection.isColumnEncryptionSettingEnabled());
        }
    }

    /**
     * Determines if parameters should be transparently encrypted (based on SQLServerStatement and the connection string
     * settings).
     * 
     * @return true if the value should be transparently encrypted, false otherwise.
     */
    static boolean shouldHonorAEForParameters(SQLServerStatementColumnEncryptionSetting stmtColumnEncryptionSetting,
            SQLServerConnection connection) {
        // Command leve setting trumps all
        switch (stmtColumnEncryptionSetting) {
            case Disabled:
            case ResultSetOnly:
                return false;
            case Enabled:
                return true;
            default:
                // Check connection level setting!
                assert SQLServerStatementColumnEncryptionSetting.UseConnectionSetting == stmtColumnEncryptionSetting : "Unexpected value for command level override";
                return (connection != null && connection.isColumnEncryptionSettingEnabled());
        }
    }

    static void validateMoneyRange(BigDecimal bd, JDBCType jdbcType) throws SQLServerException {
        if (null == bd)
            return;

        switch (jdbcType) {
            case MONEY:
                if ((1 != bd.compareTo(SSType.MAX_VALUE_MONEY)) && (-1 != bd.compareTo(SSType.MIN_VALUE_MONEY))) {
                    return;
                }
                break;
            case SMALLMONEY:
                if ((1 != bd.compareTo(SSType.MAX_VALUE_SMALLMONEY))
                        && (-1 != bd.compareTo(SSType.MIN_VALUE_SMALLMONEY))) {
                    return;
                }
                break;
            default:
                break;
        }
        MessageFormat form = new MessageFormat(SQLServerException.getErrString("R_valueOutOfRange"));
        Object[] msgArgs = {jdbcType};
        throw new SQLServerException(form.format(msgArgs), null);
    }

    static int getValueLengthBaseOnJavaType(Object value, JavaType javaType, Integer precision, Integer scale,
            JDBCType jdbcType) throws SQLServerException {
        switch (javaType) {
            // when the value of setObject() is null, the javaType stays
            // as OBJECT. We need to get the javaType base on jdbcType
            case OBJECT:
                switch (jdbcType) {
                    case DECIMAL:
                    case NUMERIC:
                        javaType = JavaType.BIGDECIMAL;
                        break;
                    case TIME:
                        javaType = JavaType.TIME;
                        break;
                    case TIMESTAMP:
                        javaType = JavaType.TIMESTAMP;
                        break;
                    case DATETIMEOFFSET:
                        javaType = JavaType.DATETIMEOFFSET;
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }

        switch (javaType) {
            case STRING:
                if (JDBCType.GUID == jdbcType) {
                    String guidTemplate = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX";
                    return ((null == value) ? 0 : guidTemplate.length());
                } else if (JDBCType.TIMESTAMP == jdbcType || JDBCType.TIME == jdbcType
                        || JDBCType.DATETIMEOFFSET == jdbcType) {
                    return ((null == scale) ? TDS.MAX_FRACTIONAL_SECONDS_SCALE : scale);
                } else if (JDBCType.BINARY == jdbcType || JDBCType.VARBINARY == jdbcType) {
                    return ((null == value) ? 0 : (ParameterUtils.HexToBin((String) value).length));
                } else {
                    return ((null == value) ? 0 : ((String) value).length());
                }
            case BYTEARRAY:
                return ((null == value) ? 0 : ((byte[]) value).length);
            case BIGDECIMAL:
                int length;
                if (null == precision) {
                    if (null == value) {
                        length = 0;
                    } else {
                        if (0 == ((BigDecimal) value).intValue()) {
                            String s = "" + value;
                            s = s.replaceAll("\\-", "");
                            if (s.startsWith("0.")) {
                                // remove the leading zero, eg., for 0.32, the precision should be 2 and not 3
                                s = s.replaceAll("0\\.", "");
                            } else {
                                s = s.replaceAll("\\.", "");
                            }
                            length = s.length();
                        }
                        // if the value is in scientific notation format
                        else if (("" + value).contains("E")) {
                            DecimalFormat dform = new DecimalFormat("###.#####");
                            String s = dform.format(value);
                            s = s.replaceAll("\\.", "");
                            s = s.replaceAll("\\-", "");
                            length = s.length();
                        } else {
                            length = ((BigDecimal) value).precision();
                        }
                    }
                } else {
                    length = precision;
                }
                return length;
            case TIMESTAMP:
            case TIME:
            case DATETIMEOFFSET:
                return ((null == scale) ? TDS.MAX_FRACTIONAL_SECONDS_SCALE : scale);
            case CLOB:
                return ((null == value) ? 0 : (DataTypes.NTEXT_MAX_CHARS * 2));
            case NCLOB:
            case READER:
                return ((null == value) ? 0 : DataTypes.NTEXT_MAX_CHARS);
            default:
                return 0;
        }
    }

    // If the access token is expiring within next 10 minutes, lets just re-create a token for this connection attempt.
    // If the token is expiring within the next 45 mins, try to fetch a new token if there is no thread already doing
    // it.
    // If a thread is already doing the refresh, just use the existing token and proceed.
    static synchronized boolean checkIfNeedNewAccessToken(SQLServerConnection connection, Date accessTokenExpireDate) {
        Date now = new Date();

        // if the token's expiration is within the next 45 mins
        // 45 mins * 60 sec/min * 1000 millisec/sec
        if ((accessTokenExpireDate.getTime() - now.getTime()) < (45 * 60 * 1000)) {

            // within the next 10 mins
            if ((accessTokenExpireDate.getTime() - now.getTime()) < (10 * 60 * 1000)) {
                return true;
            } else {
                // check if another thread is already updating the access token
                if (connection.attemptRefreshTokenLocked) {
                    return false;
                } else {
                    connection.attemptRefreshTokenLocked = true;
                    return true;
                }
            }
        }
        return false;
    }

    static final boolean use43Wrapper;

    static {
        boolean supportJDBC43 = true;
        try {
            DriverJDBCVersion.checkSupportsJDBC43();
        } catch (UnsupportedOperationException e) {
            supportJDBC43 = false;
        }

        double jvmVersion = Double.parseDouble(Util.SYSTEM_SPEC_VERSION);

        use43Wrapper = supportJDBC43 && (9 <= jvmVersion);
    }

    // if driver is for JDBC 43 and jvm version is 9 or higher, then always return as SQLServerConnection43,
    // otherwise return SQLServerConnection
    static boolean use43Wrapper() {
        return use43Wrapper;
    }

    /**
     * Escapes single quotes (') in object name to convert and pass it as String safely.
     * 
     * @param name
     *        Object name to be passed as String
     * @return Converted object name
     */
    static String escapeSingleQuotes(String name) {
        return name.replace("'", "''");
    }

    /**
     * Make SPN name
     * 
     * @param con
     * @param server
     * @param port
     * @return
     * @throws SQLServerException
     */
    private static String makeSpn(SQLServerConnection con, String server, int port) throws SQLServerException {
        StringBuilder spn = new StringBuilder("MSSQLSvc/");
        // Format is MSSQLSvc/myhost.domain.company.com:1433
        // FQDN must be provided
        if (con.serverNameAsACE()) {
            spn.append(IDN.toASCII(server));
        } else {
            spn.append(server);
        }
        spn.append(":");
        spn.append(port);
        return spn.toString();
    }

    private static final Pattern SPN_PATTERN = Pattern.compile("MSSQLSvc/(.*):([^:@]+)(@.+)?",
            Pattern.CASE_INSENSITIVE);

    /**
     * JVM Specific implementation to decide whether a realm is valid or not
     */
    interface RealmValidator {
        boolean isRealmValid(String realm);
    }

    private static RealmValidator validator;

    /**
     * Get validator to validate REALM for given JVM.
     *
     * @return a not null realm validator.
     */
    private static RealmValidator getRealmValidator() {
        if (validator != null) {
            return validator;
        }

        validator = new RealmValidator() {
            @Override
            public boolean isRealmValid(String realm) {
                try {
                    return DNSKerberosLocator.isRealmValid(realm);
                } catch (NamingException err) {
                    return false;
                }
            }
        };
        return validator;
    }

    /**
     * Try to find a REALM in the different parts of a host name.
     *
     * @param realmValidator
     *        a function that return true if REALM is valid and exists
     * @param hostname
     *        the name we are looking a REALM for
     * @return the realm if found, null otherwise
     */
    private static String findRealmFromHostname(RealmValidator realmValidator, String hostname) {
        if (hostname == null) {
            return null;
        }
        int index = 0;
        while (index != -1 && index < hostname.length() - 2) {
            String realm = hostname.substring(index);
            if (realmValidator.isRealmValid(realm)) {
                return realm.toUpperCase();
            }
            index = hostname.indexOf(".", index + 1);
            if (index != -1) {
                index = index + 1;
            }
        }
        return null;
    }

    /**
     * Enrich SPN with Realm
     * 
     * @param spn
     * @param allowHostnameCanonicalization
     * @return
     */
    static String enrichSpnWithRealm(String spn, boolean allowHostnameCanonicalization) {
        if (spn == null) {
            return spn;
        }
        Matcher m = SPN_PATTERN.matcher(spn);
        if (!m.matches()) {
            return spn;
        }
        if (m.group(3) != null) {
            // Realm is already present, no need to enrich, the job has already been done
            return spn;
        }
        String dnsName = m.group(1);
        String portOrInstance = m.group(2);
        RealmValidator realmValidator = getRealmValidator();
        String realm = findRealmFromHostname(realmValidator, dnsName);
        if (realm == null && allowHostnameCanonicalization) {
            // We failed, try with canonical host name to find a better match
            try {
                String canonicalHostName = InetAddress.getByName(dnsName).getCanonicalHostName();
                realm = findRealmFromHostname(realmValidator, canonicalHostName);
                // Since we have a match, our hostname is the correct one (for instance of server
                // name was an IP), so we override dnsName as well
                dnsName = canonicalHostName;
            } catch (UnknownHostException cannotCanonicalize) {
                // ignored, but we are in a bad shape
            }
        }
        if (realm == null) {
            return spn;
        } else {
            StringBuilder sb = new StringBuilder("MSSQLSvc/");
            sb.append(dnsName).append(":").append(portOrInstance).append("@").append(realm.toUpperCase(Locale.ENGLISH));
            return sb.toString();
        }
    }

    /**
     * Get SPN from connection string if provided or build a generic one
     * 
     * @param con
     * @return
     * @throws SQLServerException
     */
    static String getSpn(SQLServerConnection con) throws SQLServerException {
        if (null == con || null == con.activeConnectionProperties) {
            return null;
        }

        String spn;
        String userSuppliedServerSpn = con.activeConnectionProperties
                .getProperty(SQLServerDriverStringProperty.SERVER_SPN.toString());
        if (null != userSuppliedServerSpn) {
            // serverNameAsACE is true, translate the user supplied serverSPN to ASCII
            if (con.serverNameAsACE()) {
                int slashPos = userSuppliedServerSpn.indexOf("/");
                spn = userSuppliedServerSpn.substring(0, slashPos + 1)
                        + IDN.toASCII(userSuppliedServerSpn.substring(slashPos + 1));
            } else {
                spn = userSuppliedServerSpn;
            }
        } else {
            spn = makeSpn(con, con.currentConnectPlaceHolder.getServerName(),
                    con.currentConnectPlaceHolder.getPortNumber());
        }
        return enrichSpnWithRealm(spn, null == userSuppliedServerSpn);
    }

    static byte[] MD5(final byte[] str) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(str);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * tls-server-end-point channel binding type "tls-server-end-point:"
     */
    private static final byte[] TLS_SERVER_END_POINT_CHANNEL_BINDING = {0x74, 0x6c, 0x73, 0x2d, 0x73, 0x65, 0x72, 0x76,
            0x65, 0x72, 0x2d, 0x65, 0x6e, 0x64, 0x2d, 0x70, 0x6f, 0x69, 0x6e, 0x74, 0x3a};

    static byte[] generateChannelBinding(final SQLServerConnection con) {
        byte[] cert = null;
        try {
            if (null != con.tdsChannel.sslSocket) {
                Certificate[] certs = con.tdsChannel.sslSocket.getSession().getPeerCertificates();
                if (null != certs) {
                    cert = certs[0].getEncoded();
                }
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(cert);
                byte[] hash = md.digest();

                ByteBuffer cb = ByteBuffer.allocate(20 + TLS_SERVER_END_POINT_CHANNEL_BINDING.length + hash.length)
                        .order(ByteOrder.LITTLE_ENDIAN);

                cb.putInt(0); // initiator_addrtype
                cb.putInt(0); // initiator_address length
                cb.putInt(0); // acceptor_addrtype
                cb.putInt(0); // acceptor_addrtype
                cb.putInt(hash.length + TLS_SERVER_END_POINT_CHANNEL_BINDING.length); // application data length

                cb.put(TLS_SERVER_END_POINT_CHANNEL_BINDING, 0, TLS_SERVER_END_POINT_CHANNEL_BINDING.length);
                cb.put(hash, 0, hash.length);
                return MD5(cb.array());
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }
}


final class SQLIdentifier {
    // Component names default to empty string (rather than null) for consistency
    // with API behavior which returns empty string (rather than null) when the
    // particular value is not present.

    private String serverName = "";

    final String getServerName() {
        return serverName;
    }

    final void setServerName(String name) {
        serverName = name;
    }

    private String databaseName = "";

    final String getDatabaseName() {
        return databaseName;
    }

    final void setDatabaseName(String name) {
        databaseName = name;
    }

    private String schemaName = "";

    final String getSchemaName() {
        return schemaName;
    }

    final void setSchemaName(String name) {
        schemaName = name;
    }

    private String objectName = "";

    final String getObjectName() {
        return objectName;
    }

    final void setObjectName(String name) {
        objectName = name;
    }

    final String asEscapedString() {
        StringBuilder fullName = new StringBuilder(256);

        if (serverName.length() > 0)
            fullName.append("[").append(serverName).append("].");

        if (databaseName.length() > 0)
            fullName.append("[").append(databaseName).append("].");
        else
            assert 0 == serverName.length();

        if (schemaName.length() > 0)
            fullName.append("[").append(schemaName).append("].");
        else if (databaseName.length() > 0)
            fullName.append('.');

        fullName.append("[").append(objectName).append("]");

        return fullName.toString();
    }
}
