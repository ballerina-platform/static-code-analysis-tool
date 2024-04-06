/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.scan;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class BaseTest {
    protected final Path testResources = Paths.get("src", "test", "resources");
    private ByteArrayOutputStream console;
    protected PrintStream printStream;

    @BeforeMethod
    protected void setup() {
        this.console = new ByteArrayOutputStream();
        this.printStream = new PrintStream(this.console, true, StandardCharsets.UTF_8);
    }

    @AfterMethod(alwaysRun = true)
    protected void afterMethod() throws IOException {
        console.close();
        printStream.close();
    }

    protected String readOutput(boolean silent) throws IOException {
        String output = "";
        output = console.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        console.close();
        console = new ByteArrayOutputStream();
        printStream = new PrintStream(console, true, StandardCharsets.UTF_8);
        if (!silent) {
            PrintStream out = System.out;
            out.println(output);
        }
        return output;
    }
}
