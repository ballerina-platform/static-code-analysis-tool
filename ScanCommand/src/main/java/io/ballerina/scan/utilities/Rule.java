/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.ballerina.scan.utilities;

public class Rule {

    private final String ruleID;
    private final String ruleDescription;
    private boolean ruleActivated;

    public Rule(String ruleID, String ruleDescription, boolean ruleActivated) {

        this.ruleID = ruleID;
        this.ruleDescription = ruleDescription;
        this.ruleActivated = ruleActivated;
    }

    public String getRuleID() {

        return ruleID;
    }

    public String getRuleDescription() {

        return ruleDescription;
    }

    public boolean ruleIsActivated() {

        return ruleActivated;
    }

    public void setRuleIsActivated(boolean ruleActivated) {

        this.ruleActivated = ruleActivated;
    }
}
