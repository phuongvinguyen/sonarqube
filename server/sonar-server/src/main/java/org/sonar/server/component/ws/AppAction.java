/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.component.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.component.ComponentFinder.ParamNames.COMPONENT_ID_AND_COMPONENT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;

public class AppAction implements ComponentsWsAction {
  private static final String PARAM_COMPONENT_ID = "componentId";
  private static final String PARAM_COMPONENT = "component";

  private final DbClient dbClient;

  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final ComponentViewerJsonWriter componentViewerJsonWriter;

  public AppAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder, ComponentViewerJsonWriter componentViewerJsonWriter) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.componentViewerJsonWriter = componentViewerJsonWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("app")
      .setDescription("Coverage data required for rendering the component viewer.<br>" +
        "Requires the following permission: 'Browse'.")
      .setResponseExample(getClass().getResource("app-example.json"))
      .setSince("4.4")
      .setChangelog(new Change("7.6", String.format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)))
      .setInternal(true)
      .setHandler(this);

    action
      .createParam(PARAM_COMPONENT_ID)
      .setDescription("Component ID")
      .setDeprecatedSince("6.4")
      .setDeprecatedKey("uuid", "6.4")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setSince("6.4");

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setSince("6.6")
      .setInternal(true)
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);

    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id")
      .setSince("7.1")
      .setInternal(true)
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession session = dbClient.openSession(false)) {
      ComponentDto component = loadComponent(session, request);
      userSession.checkComponentPermission(UserRole.USER, component);

      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      componentViewerJsonWriter.writeComponent(json, component, userSession, session);
      appendPermissions(json, userSession);
      componentViewerJsonWriter.writeMeasures(json, component, session);
      json.endObject();
      json.close();
    }
  }

  private ComponentDto loadComponent(DbSession dbSession, Request request) {
    String componentUuid = request.param(PARAM_COMPONENT_ID);
    String branch = request.param(PARAM_BRANCH);
    String pullRequest = request.param(PARAM_PULL_REQUEST);
    checkArgument(componentUuid == null || (branch == null && pullRequest == null), "Parameter '%s' cannot be used at the same time as '%s' or '%s'", PARAM_COMPONENT_ID,
      PARAM_BRANCH, PARAM_PULL_REQUEST);
    if (branch == null && pullRequest == null) {
      return componentFinder.getByUuidOrKey(dbSession, componentUuid, request.param(PARAM_COMPONENT), COMPONENT_ID_AND_COMPONENT);
    }
    return componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, request.mandatoryParam(PARAM_COMPONENT), branch, pullRequest);
  }

  private static void appendPermissions(JsonWriter json, UserSession userSession) {
    json.prop("canMarkAsFavorite", userSession.isLoggedIn());
  }

}
