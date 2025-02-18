// Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

class A {
    private string c; // warning
}

public class A2 {
    int a = 1;
    public string b = "";
    private string c; // warning
}

public class A3 {
    int a;
    public string b;
    private string c;
    private boolean d;
    private boolean? e; // warning
    private int|boolean f = 0;
    private boolean g = false; // warning
    public string h;

    function init() {
        self.a = 0;
        self.b = "";
        self.c = "";
    }

    public function test(boolean d) {
        self.d = d;
    }

    public function test2(boolean? e) {
        if (e is ()) {
            return;
        }
        self.d = e;
    }

    public function test3(boolean e) {
        if (self.f is int) {
            return;
        }
        self.d = e;
    }
}

class A4 {
    private string c = "";
    private string d = "";

    function init() {
        self.test(self.d);
    }

    public function test(string s) {
        self.c = s;
    }
}

service class SA {
    private string b; // warning
    private string c = "";
    private string d = "";

    function init() {
        self.test(self.d);
    }

    public function test(string s) {
        self.c = s;
    }
}
