// Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

# Identifier of a `bal scan` rule: `ballerina:<number>` for a core rule, or `<org>/<package>:<number>` for a rule
# contributed by a package's analyzer. The package's compiler plugin checks it against the rules available to the
# importing package.
public type RuleId string;

# Accepts `bal scan` findings reported within the annotated construct.
#
# + rules - The rules being accepted; every rule when absent
# + reason - Why the findings are accepted
public type SuppressConfig record {|
    [RuleId, RuleId...] rules?;
    string reason?;
|};

# Suppresses `bal scan` findings reported within the annotated construct.
#
# The annotation may be attached more than once to the same construct, so that each rule can carry its own reason.
# A suppression on a service, class or object type does not reach findings inside its members.
public const annotation SuppressConfig[] Suppress on source var, source const, source type, source class,
    source function, source parameter, source return, source field, source service, source listener,
    source worker, source external;
