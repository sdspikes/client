// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.msf.records.inject;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * Defines qualifiers for dependency injection.
 */
public class Qualifiers {

    // .diagnostics

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface HealthEventBus {}

    // .events

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CrudEventBusBuilder {}

    // .net

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OpenMrsRootUrl {}

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OpenMrsUser {}

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OpenMrsPassword {}

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PackageServerRootUrl {}

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface XformUpdateClientCache {}

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IncrementalObservationUpdate {}

    private Qualifiers() {}
}
