/*
 * Microsoft JDBC Driver for SQL Server Copyright(c) Microsoft Corporation All rights reserved. This program is made
 * available under the terms of the MIT License. See the LICENSE file in the project root for more information.
 */
package com.microsoft.sqlserver.jdbc.fedauth;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.microsoft.sqlserver.jdbc.RandomUtil;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.microsoft.sqlserver.jdbc.TestUtils;
import com.microsoft.sqlserver.testframework.AbstractSQLGenerator;
import com.microsoft.sqlserver.testframework.Constants;


@RunWith(JUnitPlatform.class)
@Tag(Constants.Fedauth)
public class ConnectionEncryptionTest extends FedauthCommon {

    static String charTable = TestUtils.escapeSingleQuotes(
            AbstractSQLGenerator.escapeIdentifier(RandomUtil.getIdentifier("JDBC_ConnectionEncryption")));

    @Test
    public void testCorrectCertificate() throws SQLException {
        try (Connection connection = DriverManager.getConnection(adPasswordConnectionStr);
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT SUSER_SNAME()")) {
            rs.next();
            assertTrue(azureUserName.equals(rs.getString(1)));

            try {
                TestUtils.dropTableIfExists(charTable, stmt);
                FedauthTest.createTable(stmt, charTable);
                FedauthTest.populateCharTable(connection, charTable);
                FedauthTest.testChar(stmt, charTable);
            } finally {
                TestUtils.dropTableIfExists(charTable, stmt);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testWrongCertificate() throws SQLException {
        try (Connection connection = DriverManager
                .getConnection(adPasswordConnectionStr + ";HostNameInCertificate=WrongCertificate")) {
            fail(EXPECTED_EXCEPTION_NOT_THROWN);
        } catch (Exception e) {
            if (!(e instanceof SQLServerException)) {
                fail(EXPECTED_EXCEPTION_NOT_THROWN);
            }

            assertTrue(INVALID_EXCEPION_MSG + ": " + e.getMessage(),
                    e.getMessage().startsWith(ERR_MSG_SQL_AUTH_FAILED_SSL));
        }
    }

    // set TrustServerCertificate to true, which skips server certificate validation.
    @Test
    public void testWrongCertificateButTrustServerCertificate() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                adPasswordConnectionStr + ";HostNameInCertificate=WrongCertificate" + ";TrustServerCertificate=true");
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT SUSER_SNAME()")) {
            rs.next();
            assertTrue(azureUserName.equals(rs.getString(1)));

            try {
                TestUtils.dropTableIfExists(charTable, stmt);
                FedauthTest.createTable(stmt, charTable);
                FedauthTest.populateCharTable(connection, charTable);
                FedauthTest.testChar(stmt, charTable);
            } finally {
                TestUtils.dropTableIfExists(charTable, stmt);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @AfterAll
    public static void terminate() throws SQLException {
        try (Connection conn = DriverManager.getConnection(adPasswordConnectionStr);
                Statement stmt = conn.createStatement()) {
            TestUtils.dropTableIfExists(charTable, stmt);
        }
    }
}