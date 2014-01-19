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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.ActiveRule;

import java.lang.Override;

/**
 * Definition of a ReSharper rule.
 */
public class ReSharperRule {

    private String id;
    private boolean enabled;
    private String category;
    private String description;
    private ReSharperSeverity severity;
    private String wikiLink;

    /**
     * Constructs a @link{ReSharperRule}.
     */
    public ReSharperRule() {
    }

    @Override
    public String toString() {
        return "ReSharperRule(id=" + id + ")";
    }


    /**
     * Returns the id.
     *
     * @return The id to return.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Sets the id.
     *
     * @param id
     *          The id to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the enabled.
     *
     * @return The enabled to return.
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Sets the enabled.
     *
     * @param enabled
     *          The enabled to set.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the category.
     *
     * @return The category to return.
     */
    public String getCategory() {
        return this.category;
    }

    /**
     * Sets the category.
     *
     * @param category
     *          The category to set.
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Returns the description.
     *
     * @return The description to return.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets the description.
     *
     * @param description
     *          The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the WikiLink.
     *
     * @return The WikiLink to return.
     */
    public String getWikiLink() {
        return this.wikiLink;
    }

    /**
     * Sets the WikiLink.
     *
     * @param wikiLink
     *          The WikiLink to set.
     */
    public void setWikiLink(String wikiLink) {
        this.wikiLink = wikiLink;
    }

    /**
     * Get the resharper severity of this rule
     *
     * @return the resharper severity
     */
    public ReSharperSeverity getSeverity() {
        return severity;
    }

    /**
     * Set the resharper severity of this rule
     *
     * @param severity
     *          resharper severity
     */
    public void setSeverity(ReSharperSeverity severity) {
        this.severity = severity;
    }


    /**
     * Set the resharper severity of this rule based on the sonar priority
     *
     * @param sonarPriority
     *          sonar priority
     */
    public void setSonarPriority(RulePriority sonarPriority){
        ReSharperRule.ReSharperSeverity resharperPriority = TranslateSonarPriorityIntoResharperSeverity(sonarPriority);
        setSeverity(resharperPriority);
    }

    public RulePriority getSonarPriority()
    {
        RulePriority sonarPriority = TranslateResharperPriorityIntoSonarSeverity(getSeverity());
        return sonarPriority;
    }


    //http://www.jetbrains.com/resharper/webhelp/Reference__Options__Code_Inspection__Inspection_Severity.html
    //http://www.jetbrains.com/resharper/webhelp/Code_Analysis__Code_Highlighting.html
    public enum ReSharperSeverity {
        ERROR,   //Errors have the highest priority of all - they prevent your code from compiling.
        WARNING,  // ReSharper provides you with warnings that do not prevent your code from compiling but may nevertheless represent serious coding inefficiencies.
        SUGGESTION,  //Code suggestions provide insights into code structure, drawing your attention to things that aren't necessarily bad or wrong, but probably useful to know.
        INFO, //See IssueType "InvocationIsSkipped" has undocumented "INFO" severity -- http://youtrack.jetbrains.com/issue/RSRP-390375
        HINT, //This is the lowest possible severity level. A hint simply brings your attention to a particular code detail and recommends a way of improvement.
        DO_NOT_SHOW
    }


    private RulePriority TranslateResharperPriorityIntoSonarSeverity(ReSharperSeverity severity) {

        switch (severity) {
            case ERROR:
                return RulePriority.BLOCKER;
            case WARNING:
                return RulePriority.CRITICAL;
            case SUGGESTION:
                return RulePriority.MINOR;
            case HINT:
            case INFO:
            case DO_NOT_SHOW:
                return RulePriority.INFO ;
            default:
                return null;
        }
    }

    private ReSharperSeverity TranslateSonarPriorityIntoResharperSeverity(RulePriority priority) {

        switch (priority) {
            case BLOCKER:
                return ReSharperSeverity.ERROR;
            case CRITICAL:
            case MAJOR:
                return ReSharperSeverity.WARNING;
            case MINOR:
                return ReSharperSeverity.SUGGESTION;
            case INFO:
                return ReSharperSeverity.HINT;
            default:
                return null;
        }

    }

    public Rule toSonarRule() {

        /*
            Rule			<==		IssueType
            key				<==		Id
            <name>			<==		Id
            <configKey>		<==		"ReSharperInspectCode#"+ Id
            <description>   <==		Description + Wiki link + "<br/>(Category: " + Category + ")"
         *
         * WikiUrl="http://confluence.jetbrains.net/display/ReSharper/Use+'var'+keyword+when+initializer+explicitly+declares+type"
        */


//
//        <rule key="ClassNeverInstantiated.Global">
//          <name><![CDATA[ClassNeverInstantiated.Global]]></name>
//          <configKey><![CDATA[ReSharperInspectCode#ClassNeverInstantiated.Global]]></configKey>
//          <description><![CDATA[Class is never instantiated: Non-private accessibility<br/>(Category: Potential Code Quality Issues)]]></description>
//          <enabled>true</enabled>
//          <priority>MINOR</priority>
//        </rule>


        //if the description is empty, the import of rules will
        //fail, so set the description to the id (name) if it doesn't exist
        String desc = getDescription();
        if (StringUtils.isBlank(desc))
        {
            desc = getId();
        }

        if (!StringUtils.isBlank(wikiLink))
        {
            desc += "<br /><a href='"+wikiLink+"'>" + wikiLink + "</a>";
        }

        if (!StringUtils.isBlank(category))
        {
            desc += "<br />(Category: "+category + ")";
        }

        Rule sonarRule = Rule.create()
                .setKey(getKey())
                .setName(getId())
                .setConfigKey("ReSharperInspectCode#" + getId())
                .setDescription(desc)
                .setSeverity(getSonarPriority());

       return sonarRule;
    }

    public static ReSharperRule createFromActiveRule(ActiveRule activeRule) {
        //  input:
        //        <rule key="ConvertToConstant.Global">
        //          <name><![CDATA[ConvertToConstant.Global]]></name>
        //          <configKey><![CDATA[ReSharperInspectCode#ConvertToConstant.Global]]></configKey>
        //          <description><![CDATA[Convert local variable or field to constant: Non-private accessibility<br/>(Category: Common Practices and Code Improvements)]]></description>
        //        </rule>


        //Note: I don't extract out the Category and WikiUrl since they are just pushed back into the description
        //when rehydrating the Rule object.

        // Extracts the rule's information
        Rule rule = activeRule.getRule();
        String name = rule.getName();
        String rawDesc = rule.getDescription();

        // Creates the ReSharper rule
        ReSharperRule resharperRule = new ReSharperRule();
        resharperRule.setEnabled(rule.isEnabled());
        resharperRule.setId(name);
        resharperRule.setDescription(rawDesc);

        RulePriority priority = activeRule.getSeverity();
        if (priority != null) {
            resharperRule.setSonarPriority(priority);
        }

        return resharperRule;
    }
    
    public String getKey() {
    	return getId().replaceAll(":", "_");
    }

}
