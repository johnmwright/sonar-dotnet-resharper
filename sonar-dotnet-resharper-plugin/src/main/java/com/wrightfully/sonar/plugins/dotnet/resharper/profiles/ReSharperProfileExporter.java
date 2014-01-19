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
package com.wrightfully.sonar.plugins.dotnet.resharper.profiles;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.SonarException;
import com.wrightfully.sonar.plugins.dotnet.resharper.ReSharperConstants;
import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperRule;

import java.io.IOException;
import java.io.Writer;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that allows to export a Sonar profile into a ReSharper rule definition file.
 */
public class ReSharperProfileExporter extends ProfileExporter {

    public static class CSharpRegularReSharperProfileExporter extends ReSharperProfileExporter {
        public CSharpRegularReSharperProfileExporter() {
            super("cs");
        }
    }

    public static class VbNetRegularReSharperProfileExporter extends ReSharperProfileExporter {
        public VbNetRegularReSharperProfileExporter() {
            super("vbnet");
        }
    }

    protected ReSharperProfileExporter(String languageKey) {
        super(ReSharperConstants.REPOSITORY_KEY + "-" + languageKey, ReSharperConstants.REPOSITORY_NAME);
        setSupportedLanguages(languageKey);
        setMimeType("application/xml");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportProfile(RulesProfile profile, Writer writer) {
        try {
            printRules(profile, writer);
        } catch (IOException e) {
            throw new SonarException("Error while generating the ReSharper profile to export: " + profile, e);
        }
    }

    private void printRules(RulesProfile profile, Writer writer) throws IOException {
        //Create a file that matches the format of the ReSharper inspectcode.exe output

        writer.append("<Report>\n");
        writer.append("  <IssueTypes>\n");

        List<ActiveRule> activeRules = profile.getActiveRulesByRepository(getKey());
        List<ReSharperRule> rules = transformIntoReSharperRules(activeRules);

        // print out each rule
        for (ReSharperRule rule : rules) {
            printRule(writer, rule);
        }

        writer.append("  </IssueTypes>\n");
        writer.append("</Report>");
    }


    private void printRule(Writer writer, ReSharperRule resharperRule) throws IOException {
        // This is generally what the output will look like:
        //        <IssueType Id="ClassNeverInstantiated.Global"
        //                   Enabled="True"
        //                   Description="Class is never instantiated: Non-private accessibility"
        //                   Severity="SUGGESTION" />

 
        writer.append("    <IssueType");
        writer.append(" Id=\"");
        StringEscapeUtils.escapeXml(writer, resharperRule.getId());
        
        writer.append("\" Enabled=\"");
        StringEscapeUtils.escapeXml(writer, String.valueOf(resharperRule.isEnabled()));

        String category = resharperRule.getCategory();
        if (category != null && !StringUtils.isBlank(category)) {
            writer.append("\" Category=\"");
            StringEscapeUtils.escapeXml(writer, category);
        }

        String wiki = resharperRule.getWikiLink();
        if (wiki != null && !StringUtils.isBlank(wiki)) {
            writer.append("\" WikiUrl=\"");
            StringEscapeUtils.escapeXml(writer, wiki);
        }

        writer.append("\" Description=\"");
        StringEscapeUtils.escapeXml(writer, resharperRule.getDescription());
        writer.append("\" Severity=\"");
        StringEscapeUtils.escapeXml(writer, resharperRule.getSeverity().toString());
        writer.append("\"/>\n");
    }

    private List<ReSharperRule> transformIntoReSharperRules(List<ActiveRule> activeRulesByPlugin) {
        List<ReSharperRule> result = new ArrayList<ReSharperRule>();

        for (ActiveRule activeRule : activeRulesByPlugin) {
            ReSharperRule resharperRule = ReSharperRule.createFromActiveRule(activeRule);
            result.add(resharperRule);
        }
        return result;
    }



}
