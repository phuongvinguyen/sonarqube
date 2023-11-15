/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;



public class RenameBuildDateInSnapshotsTest {
  private static final String TABLE_NAME = "snapshots";

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RenameBuildDateInSnapshots.class);
  private final RenameBuildDateInSnapshots underTest = new RenameBuildDateInSnapshots(db.database());

  @Test
  public void execute_whenExecuted_shouldRenameColumn() throws SQLException {
    assertColumnExists("build_date");
    underTest.execute();
    assertColumnExists("analysis_date");
  }

  @Test
  public void execute_whenExecutedTwice_shouldNotFail() throws SQLException {
    assertColumnExists("build_date");
    underTest.execute();
    underTest.execute();
    assertColumnExists("analysis_date");
  }

  private void assertColumnExists(String columnName) {
    db.assertColumnDefinition(TABLE_NAME, columnName, Types.BIGINT, null, true);
  }
}
