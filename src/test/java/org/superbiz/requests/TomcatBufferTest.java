/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.superbiz.requests;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.CRL;

public class TomcatBufferTest {

    @Test
    public void test() {
        Client client = new Client();

        client.doRequest();
        Assert.assertTrue(client.isResponse200());
        Assert.assertTrue(client.isResponseBodyOK());
    }

    private class Client extends SimpleHttpClient {

        private Exception doRequest() {

            Tomcat tomcat = new Tomcat();
            Context root = tomcat.addContext("", TEMP_DIR);
            Tomcat.addServlet(root, "TesterServlet", new TesterServlet());
            root.addServletMapping("/test", "TesterServlet");

            try {
                tomcat.start();
                setPort(tomcat.getConnector().getLocalPort());
                setRequestPause(20);

                // Open connection
                connect();

                String[] request = new String[1];
                request[0] =
                        "POST /test HTTP/1.1" + CRLF +
                                0x01 + "Transfer-Encoding: chunked" + CRLF +
                                "Content-Length: 35" + CRLF +
                                "host: localhost:8080" + CRLF +
                                "Connection: close" + CRLF +
                                "0" + CRLF + CRLF +
                                "GET /robots.txt HTTP/1.1" + CRLF +
                                "X:X" + CRLF +
                                CRLF;

                setRequest(request);
                processRequest(); // blocks until response has been read

                // Close the connection
                disconnect();
            } catch (Exception e) {
                return e;
            } finally {
                try {
                    tomcat.destroy();
                } catch (Exception e) {
                    // no-op
                }
            }
            return null;
        }

        @Override
        public boolean isResponseBodyOK() {
            if (getResponseBody() == null) {
                return false;
            }
            if (!getResponseBody().contains("POST")) {
                return false;
            }
            return true;
        }

    }

    private class TesterServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            final PrintWriter writer = resp.getWriter();
            writer.println("GET");
            writer.flush();
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            final PrintWriter writer = resp.getWriter();
            writer.println("POST");
            writer.flush();
        }
    }
}
