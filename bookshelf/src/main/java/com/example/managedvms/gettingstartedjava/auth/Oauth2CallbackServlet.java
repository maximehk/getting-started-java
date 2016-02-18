/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.managedvms.gettingstartedjava.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.plus.PlusScopes;

import com.example.managedvms.gettingstartedjava.util.DatastoreHttpServlet;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// [START example]
@WebServlet(name = "oauth2callback", value = "/oauth2callback")
@SuppressWarnings("serial")
public class Oauth2CallbackServlet extends DatastoreHttpServlet {

  private GoogleAuthorizationCodeFlow flow;
  private static final Collection<String> SCOPE =
      Arrays.asList(PlusScopes.USERINFO_EMAIL, PlusScopes.PLUS_LOGIN);
  private static final String LOGIN_API_URL = "https://www.googleapis.com/oauth2/v1/userinfo";
  private static final JsonFactory JSON_FACTORY = new JacksonFactory();
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException,
      ServletException {
    // Ensure that this is no request forgery going on, and that the user
    // sending us this connect request is the user that was supposed to.
    Set<String> names = listSessionVariables();
    if (
        !names.contains("state") ||
        !req.getParameter("state").equals(getSessionVariable("state"))) {
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      resp.getWriter().print("Invalid state parameter.");
      return;
    }
    // remove one-time use state
    // req.getSession().removeAttribute("state");
    // deleteSessionVariable("state");
    flow =
        new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT,
            JSON_FACTORY,
            System.getenv("CLIENT_ID"),
            System.getenv("CLIENT_SECRET"),
            SCOPE)
        .build();
    final TokenResponse tokenResponse =
        flow.newTokenRequest(req.getParameter("code"))
        .setRedirectUri(System.getenv("CALLBACK_URL"))
        .execute();

    // keep track of the token
    // req.getSession().setAttribute("token", tokenResponse.toString());
    setSessionVariable("token", tokenResponse.toString());
    final Credential credential = flow.createAndStoreCredential(tokenResponse, null);
    final HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(credential);
    // Make an authenticated request
    final GenericUrl url = new GenericUrl(LOGIN_API_URL);
    final HttpRequest request = requestFactory.buildGetRequest(url);
    request.getHeaders().setContentType("application/json");

    final String jsonIdentity = request.execute().parseAsString();
    @SuppressWarnings("unchecked")
    HashMap<String, String> userIdResult =
        new ObjectMapper().readValue(jsonIdentity, HashMap.class);
    // from this map, extract the relevant profile info and store it in the session
    // req.getSession().setAttribute("userEmail", userIdResult.get("email"));
    // req.getSession().setAttribute("userId", userIdResult.get("id"));
    // req.getSession().setAttribute("userImageUrl",
    // userIdResult.get("picture"));
    setSessionVariable("userEmail", userIdResult.get("email"));
    setSessionVariable("userId", userIdResult.get("id"));
    setSessionVariable("userImageUrl", userIdResult.get("picture"));
    // req.getRequestDispatcher(
    // req.getSession().getAttribute("loginDestination").toString()).forward(req, resp);
    listSessionVariables();
    req.getRequestDispatcher(getSessionVariable("loginDestination")).forward(req, resp);
  }
}
// [END example]
