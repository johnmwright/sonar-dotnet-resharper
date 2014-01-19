/*
 * Sonar .NET Plugin :: ReSharper
 * Copyright (C) 2013 John M. Wright
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.wrightfully.sonar.plugins.dotnet.resharper;

import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperProfileExporter;


import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.*;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.Violation;
import org.sonar.plugins.dotnet.api.DotNetResourceBridge;
import org.sonar.plugins.dotnet.api.DotNetResourceBridges;
import org.sonar.plugins.dotnet.api.microsoft.MicrosoftWindowsEnvironment;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioSolution;
import org.sonar.plugins.dotnet.api.utils.ResourceHelper;
import org.sonar.test.TestUtils;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.test.TestUtils;

import java.io.File;
import java.lang.Exception;
import java.nio.charset.Charset;
import java.io.IOException;
import java.io.StringWriter;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;


import org.xml.sax.SAXException;


import static org.fest.assertions.Assertions.assertThat;



public class ReSharperProfileExporterTest {

    @Test
    public void testSimpleReSharpersRulesToExport() throws IOException, SAXException {
        RulesProfile profile = RulesProfile.create("Sonar way", "cs");
        profile.activateRule(Rule.create(ReSharperConstants.REPOSITORY_KEY+"-cs", "ClassNeverInstantiated.Global", "ClassNeverInstantiated.Global")
                .setDescription("Class is never instantiated: Non-private accessibility<br />(Category: Potential Code Quality Issues)")
                .setConfigKey("ReSharperInspectCode#ClassNeverInstantiated.Global"), RulePriority.MINOR);
        profile.activateRule(Rule.create(ReSharperConstants.REPOSITORY_KEY+"-cs", "ClassWithVirtualMembersNeverInherited.Global", "ClassWithVirtualMembersNeverInherited.Global")
                .setDescription("Class with virtual(overridable) members never inherited: Non-private accessibility<br />(Category: Redundancies in Symbol Declarations)")
                .setConfigKey("ReSharperInspectCode#ClassWithVirtualMembersNeverInherited.Global"), RulePriority.MINOR);
        profile.activateRule(Rule.create(ReSharperConstants.REPOSITORY_KEY+"-cs", "MemberCanBeMadeStatic.Global", "MemberCanBeMadeStatic.Global")
                .setDescription("Member can be made static(shared): Non-private accessibility<br />(Category: Common Practices and Code Improvements)")
                .setConfigKey("ReSharperInspectCode#MemberCanBeMadeStatic.Global"), RulePriority.INFO);
        profile.activateRule(Rule.create(ReSharperConstants.REPOSITORY_KEY+"-cs", "CSharpWarnings::CS0162", "CSharpWarnings::CS0162")
                .setDescription("CS0162:Code is unreachable<br />(Category: Compiler Warnings)")
                .setConfigKey("ReSharperInspectCode#CSharpWarnings::CS0162"), RulePriority.INFO);
        StringWriter writer = new StringWriter();
        ReSharperProfileExporter exporter = new ReSharperProfileExporter.CSharpRegularReSharperProfileExporter();
        assertThat(exporter.getKey()).isEqualTo("resharper-cs");
        assertThat(exporter.getSupportedLanguages()).containsOnly("cs");

        exporter.exportProfile(profile, writer);

        String createdValue = writer.toString();
        String expectedValue =     TestUtils.getResourceContent("/ProfileExporter/SimpleRules.ReSharper.exported.xml");

        TestUtils.assertSimilarXml(expectedValue, createdValue);
    }

    @Test
    public void testExporterForVbNet() {
        ReSharperProfileExporter exporter = new ReSharperProfileExporter.VbNetRegularReSharperProfileExporter();
        // just test the differences with C#
        assertThat(exporter.getKey()).isEqualTo("resharper-vbnet");
        assertThat(exporter.getSupportedLanguages()).containsOnly("vbnet");
    }
}