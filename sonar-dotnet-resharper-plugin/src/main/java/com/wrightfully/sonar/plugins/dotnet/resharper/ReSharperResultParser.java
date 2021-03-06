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

import org.apache.commons.io.IOUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.dotnet.api.microsoft.MicrosoftWindowsEnvironment;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioSolution;
import org.sonar.plugins.dotnet.api.utils.StaxParserUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Parses the reports generated by a ReSharper analysis.
 */
public class ReSharperResultParser implements BatchExtension {

    private static final Logger LOG = LoggerFactory.getLogger(ReSharperResultParser.class);

    private final VisualStudioSolution vsSolution;
    private VisualStudioProject vsProject;
    private Project project;
    private SensorContext context;
    private RuleFinder ruleFinder;
    private String repositoryKey;
    private Boolean includeAllFiles;

    private final static String issuesLink = "https://jira.codehaus.org/browse/SONARPLUGINS/component/16153";
    private final static String missingIssueTypesRuleKey = "ReSharperInspectCode#Sonar.UnknownIssueType";

    /**
     * Constructs a @link{ReSharperResultParser}.
     */
    public ReSharperResultParser(MicrosoftWindowsEnvironment env, Project project, SensorContext context, RuleFinder ruleFinder, ReSharperConfiguration configuration) {
        super();

        this.vsSolution = env.getCurrentSolution();
        if (vsSolution == null) {
            // not a .NET project
            return;
        }

        this.vsProject = vsSolution.getProjectFromSonarProject(project);

        this.project = project;
        this.context = context;
        this.ruleFinder = ruleFinder;

        String projLanguage =  project.getLanguageKey();
        repositoryKey = ReSharperConstants.REPOSITORY_KEY + "-" + projLanguage;

        includeAllFiles = configuration.getBoolean(ReSharperConstants.INCLUDE_ALL_FILES);
    }

    /**
     * Parses a processed violation file.
     *
     * @param file
     *          the file to parse
     */
    public void parse(File file) {

        SMInputFactory inputFactory = StaxParserUtils.initStax();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            SMHierarchicCursor cursor = inputFactory.rootElementCursor(new InputStreamReader(fileInputStream, project.getFileSystem().getSourceCharset()));
            SMInputCursor mainCursor = cursor.advance().childElementCursor();

            MissingIssueTypeHelper missingTypesHelper = new MissingIssueTypeHelper();

            while (mainCursor.getNext() != null) {

                String nodeName =mainCursor.getQName().getLocalPart();

                if (nodeName.equals("Issues")) {
                    parseIssuesBloc(mainCursor, missingTypesHelper);
                } else if (nodeName.equals("IssueTypes")) {
                    missingTypesHelper.setIssueTypes(mainCursor);
                }
            }

            if (missingTypesHelper.hasMissingIssues())
            {
                missingTypesHelper.logMissingIssues();
            }

            cursor.getStreamReader().closeCompletely();
        } catch (XMLStreamException e) {
            throw new SonarException("Error while reading ReSharper result file: " + file.getAbsolutePath(), e);
        } catch (FileNotFoundException e) {
            throw new SonarException("Cannot find ReSharper result file: " + file.getAbsolutePath(), e);
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
    }

    private class MissingIssueTypeHelper {

        private final Set<String> _missingIssueTypes;

        public boolean hasMissingIssues() {
            return !_missingIssueTypes.isEmpty();
        }

        public MissingIssueTypeHelper(){
            _missingIssueTypes = new HashSet<String>();
        }

        public void setIssueTypes(SMInputCursor issuesTypeCursor) throws XMLStreamException {
            SMInputCursor _issueTypeCursor = issuesTypeCursor.childElementCursor("IssueType");
            LOG.debug("Parsing IssueTypes");
            while (_issueTypeCursor.getNext() != null){
                String issueTypeId = _issueTypeCursor.getAttrValue("Id");
                StringBuilder xml =  new StringBuilder("<IssueType " );
                int attrCount = _issueTypeCursor.getAttrCount();
                for (int i = 0; i < attrCount; i++ )
                {
                    String name = _issueTypeCursor.getAttrName(i).getLocalPart();
                    String value = _issueTypeCursor.getAttrValue(i);
                    xml.append(name + "=\"" + value + "\" ");
                }
                xml.append("/>");

                String xmlOut = xml.toString();
                LOG.debug("Found IssueType " + issueTypeId + " with value " + xmlOut);

                _issueTypeCache.put(issueTypeId, xmlOut);
            }

        }

        public void addMissingIssueType(String issueTypeName){
            _missingIssueTypes.add(issueTypeName);
        }

        Map<String, String> _issueTypeCache = new HashMap<String, String>();

        public void logMissingIssues() {

            if (!hasMissingIssues())
                return;

            StringBuilder logMessageBuf = new StringBuilder( "The following IssueTypes are not known to the SonarQube ReSharper plugin.\n" +
                    "Add the following text to the 'ReSharper custom rules' property in the Settings UI to add local " +
                    "support for these rules and submit them to " + issuesLink + " so that they can be included in " +
                    "future releases.\n");

            for(String missingIssueType: _missingIssueTypes)
            {

                if (!_issueTypeCache.containsKey(missingIssueType)){
                    logMessageBuf.append( " -IssueType not found- ");
                } else {
                    String messageText = _issueTypeCache.get(missingIssueType);
                    logMessageBuf.append( messageText + "\n");
                }
            }

            String logMessage = logMessageBuf.toString();
            LOG.warn(logMessage);

            Rule currentRule = ruleFinder.find(RuleQuery.create().withRepositoryKey(repositoryKey).withConfigKey(missingIssueTypesRuleKey));

            if (currentRule != null) {
                Violation violation = Violation.create(currentRule, project);
                violation.setMessage(logMessage);
                context.saveViolation(violation);
            }  else {
                LOG.warn("Could not find rule for " + missingIssueTypesRuleKey);
            }
        }

    }

    private void parseIssuesBloc(SMInputCursor cursor, MissingIssueTypeHelper missingTypesHelper) throws XMLStreamException {
        // Cursor on <Issues>
        SMInputCursor projectsCursor = cursor.childElementCursor("Project");
        while (projectsCursor.getNext() != null) {

            //compare with vsProject to only run the current project's block
            String projectName = projectsCursor.getAttrValue("Name");
            String thisName =  vsProject.getName();
            if (projectName.equals(thisName))  {
                parseProjectBloc(projectsCursor, missingTypesHelper);
            } else {
                LOG.debug("Skipping project block due to name mismatch.  Currently analyzing '" + thisName +"', processing '" + projectName + "'");
            }
        }
    }


    private void parseProjectBloc(SMInputCursor projectCursor, MissingIssueTypeHelper missingTypesHelper) throws XMLStreamException {
        // Cursor in on <Project>
        SMInputCursor issuesCursor = projectCursor.childElementCursor("Issue");
        while (issuesCursor.getNext() != null) {

            String typeId = issuesCursor.getAttrValue("TypeId");
            String configRuleKey = "ReSharperInspectCode#" + typeId;

            LOG.debug("Searching for rule '"+configRuleKey+"' in repository '" + repositoryKey +"'");
            Rule currentRule = ruleFinder.find(RuleQuery.create().withRepositoryKey(repositoryKey).withConfigKey(configRuleKey));
            if (currentRule != null) {
                LOG.debug("Rule found: " + configRuleKey);
                createViolation(issuesCursor, currentRule);
            } else {
                LOG.warn("Could not find the following rule in the ReSharper rule repository: " + configRuleKey);
                missingTypesHelper.addMissingIssueType(typeId);
            }
        }
    }


    private void createViolation(SMInputCursor violationsCursor, Rule currentRule) throws XMLStreamException {
        String relativeFilePath = violationsCursor.getAttrValue("File");

        //Paths in the resharper results file are relative to the Solution file
        LOG.debug("createViolation for relativePath: " + relativeFilePath);
        File sourceFile = new File(vsSolution.getSolutionDir(), relativeFilePath);

        final org.sonar.api.resources.File sonarFile = org.sonar.api.resources.File.fromIOFile(sourceFile, project);

        try{
            LOG.debug("searching for sourceFile " + sourceFile.getCanonicalFile().getPath() + " - Exists: " + sourceFile.exists());
        } catch (Exception ex) {
            LOG.warn("Exception: " + ex.getMessage());
        }

        if (context.isExcluded(sonarFile)) {
            LOG.debug("File is marked as excluded, so not reporting violation: {}", sonarFile.getName());
        } else if (includeAllFiles || vsProject.contains(sourceFile)) {
            try {
                Violation violation = createViolationAgainstFile(violationsCursor, currentRule, sourceFile);
                context.saveViolation(violation);
            } catch (Exception ex){
                LOG.warn("Violation could not be saved against file, associating to VS project instead: " + sourceFile.getPath());

                Violation violation = createViolationAgainstProject(violationsCursor, currentRule, sourceFile);
                context.saveViolation(violation);
            }
        } else {
            LOG.debug("Violation not being saved for unsupported file {}", sourceFile.getName());
        }

    }


    private Violation createViolationAgainstFile(SMInputCursor violationsCursor, Rule currentRule, File sourceFile) throws Exception {
        final org.sonar.api.resources.File sonarFile = org.sonar.api.resources.File.fromIOFile(sourceFile, project);

        Violation violation = Violation.create(currentRule, sonarFile);

        String message = violationsCursor.getAttrValue("Message");

        String lineNumber = violationsCursor.getAttrValue("Line");
        if (lineNumber != null) {
            violation.setLineId(Integer.parseInt(lineNumber));

            if (!vsProject.contains(sourceFile))
            {
                message += " (for file " + sonarFile.getName();
                if (lineNumber != null) {
                    message += " line " + lineNumber;
                }
                message +=  ")";

            }
        }


        violation.setMessage(message.trim());
        return violation;
    }

    private Violation createViolationAgainstProject(SMInputCursor violationsCursor, Rule currentRule, File sourceFile) throws XMLStreamException {
        Violation violation = Violation.create(currentRule, project);
        String lineNumber = violationsCursor.getAttrValue("Line");

        String message = violationsCursor.getAttrValue("Message");

        message += " (for file " + sourceFile.getName();
        if (lineNumber != null) {
            message += " line " + lineNumber;
        }
        message +=  ")";

        violation.setMessage(message.trim());
        return violation;
    }

}
