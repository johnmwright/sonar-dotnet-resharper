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

import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperProfileImporter;

import org.sonar.api.utils.ValidationMessages;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
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
import java.io.Reader;
import java.io.StringReader;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;


import org.xml.sax.SAXException;


import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;


public class ReSharperProfileImporterTest {

    private ValidationMessages messages;
    private ReSharperProfileImporter importer;

    @Before
    public void before() {
        messages = ValidationMessages.create();
        importer = new ReSharperProfileImporter.CSharpRegularReSharperProfileImporter(newRuleFinder());
    }

    @Test
    public void testImportSimpleProfile() {
        Reader reader = new StringReader(TestUtils.getResourceContent("/ProfileExporter/SimpleRules.ReSharper.exported.xml"));
        RulesProfile profile = importer.importProfile(reader, messages);

        assertThat(messages.hasWarnings()).isFalse();
        assertThat(messages.hasErrors()).isFalse();

        assertThat(profile.getActiveRules().size()).isEqualTo(4);

        ActiveRule cniRule = profile.getActiveRuleByConfigKey(ReSharperConstants.REPOSITORY_KEY+"-cs",
                "ReSharperInspectCode#ClassNeverInstantiated.Global");
        assertNotNull(cniRule);
        assertThat(cniRule.getSeverity()).isEqualTo(RulePriority.MINOR);

        ActiveRule cvmniRule = profile.getActiveRuleByConfigKey(ReSharperConstants.REPOSITORY_KEY+"-cs",
                "ReSharperInspectCode#ClassWithVirtualMembersNeverInherited.Global");
        assertNotNull(cvmniRule);
        assertThat(cvmniRule.getSeverity()).isEqualTo(RulePriority.MINOR);


        ActiveRule mcbmsRule =  profile.getActiveRuleByConfigKey(ReSharperConstants.REPOSITORY_KEY+"-cs",
                "ReSharperInspectCode#MemberCanBeMadeStatic.Global");
        assertNotNull(mcbmsRule);
        assertThat(mcbmsRule.getSeverity()).isEqualTo(RulePriority.INFO);

        // check the name of the repo
        assertThat(profile.getActiveRules().get(0).getRepositoryKey()).isEqualTo(ReSharperConstants.REPOSITORY_KEY+"-cs");
    }

    @Test
    public void shouldCreateImporterForVbNet() {
        importer = new ReSharperProfileImporter.VbNetRegularReSharperProfileImporter(newRuleFinder());

        Reader reader = new StringReader(TestUtils.getResourceContent("/ProfileExporter/SimpleRules.ReSharper.exported.xml"));
        RulesProfile profile = importer.importProfile(reader, messages);

        // check the name of the repo
        assertThat(profile.getActiveRules().get(0).getRepositoryKey()).isEqualTo(ReSharperConstants.REPOSITORY_KEY+"-vbnet");
    }

    private RuleFinder newRuleFinder() {
        RuleFinder ruleFinder = mock(RuleFinder.class);
        when(ruleFinder.find((RuleQuery) anyObject())).thenAnswer(new Answer<Rule>() {

            public Rule answer(InvocationOnMock iom) throws Throwable {
                RuleQuery query = (RuleQuery) iom.getArguments()[0];
                Rule rule = null;
                String configKey=query.getConfigKey();
                String ruleKey=query.getKey();
                if (StringUtils.equals(configKey, "ReSharperInspectCode#ClassNeverInstantiated.Global")
                        || StringUtils.equals(ruleKey, "ClassNeverInstantiated.Global")) {
                    rule = Rule.create(query.getRepositoryKey(), "ClassNeverInstantiated.Global", "ClassNeverInstantiated.Global")
                            .setDescription("Class is never instantiated: Non-private accessibility<br />(Category: Potential Code Quality Issues)")
                            .setConfigKey("ReSharperInspectCode#ClassNeverInstantiated.Global");

                } else if (StringUtils.equals(configKey, "ReSharperInspectCode#ClassWithVirtualMembersNeverInherited.Global")
                        || StringUtils.equals(ruleKey, "ClassWithVirtualMembersNeverInherited.Global")) {
                    rule = Rule.create(query.getRepositoryKey(), "ClassWithVirtualMembersNeverInherited.Global", "ClassWithVirtualMembersNeverInherited.Global")
                            .setDescription("Class with virtual(overridable) members never inherited: Non-private accessibility<br />(Category: Redundancies in Symbol Declarations)")
                            .setConfigKey("ReSharperInspectCode#ClassWithVirtualMembersNeverInherited.Global");

                } else if (StringUtils.equals(configKey, "ReSharperInspectCode#MemberCanBeMadeStatic.Global")
                        || StringUtils.equals(ruleKey, "MemberCanBeMadeStatic.Global")) {
                    rule = Rule.create(query.getRepositoryKey(), "MemberCanBeMadeStatic.Global", "MemberCanBeMadeStatic.Global")
                            .setDescription("Member can be made static(shared): Non-private accessibility<br />(Category: Common Practices and Code Improvements)")
                            .setConfigKey("ReSharperInspectCode#MemberCanBeMadeStatic.Global");

                } else if (StringUtils.equals(configKey, "ReSharperInspectCode#MemberCanBeMadeStatic.Global")
                        || StringUtils.equals(ruleKey, "MemberCanBeMadeStatic.Global")) {
                    rule = Rule.create(query.getRepositoryKey(), "MemberCanBeMadeStatic.Global", "MemberCanBeMadeStatic.Global")
                            .setDescription("Member can be made static(shared): Non-private accessibility<br />(Category: Common Practices and Code Improvements)")
                            .setConfigKey("ReSharperInspectCode#MemberCanBeMadeStatic.Global");
                } else if (StringUtils.equals(configKey, "ReSharperInspectCode#CSharpWarnings::CS0162")
                            || StringUtils.equals(ruleKey, "CSharpWarnings__CS0162")) {
                        rule = Rule.create(query.getRepositoryKey(), "CSharpWarnings__CS0162", "CSharpWarnings::CS0162")
                                .setDescription("CS0162:Code is unreachable(Category: Compiler Warnings)")
                                .setConfigKey("ReSharperInspectCode#CSharpWarnings::CS0162");
                }
                return rule;
            }
        });
        return ruleFinder;
    }



}
