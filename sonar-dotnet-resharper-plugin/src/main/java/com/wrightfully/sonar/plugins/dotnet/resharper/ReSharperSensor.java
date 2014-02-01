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

import com.google.common.base.Joiner;
import com.wrightfully.sonar.dotnet.tools.resharper.ReSharperCommandBuilder;
import com.wrightfully.sonar.dotnet.tools.resharper.ReSharperException;
import com.wrightfully.sonar.dotnet.tools.resharper.ReSharperRunner;
import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperProfileExporter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.dotnet.api.DotNetConfiguration;
import org.sonar.plugins.dotnet.api.DotNetConstants;
import org.sonar.plugins.dotnet.api.microsoft.MicrosoftWindowsEnvironment;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioSolution;
import org.sonar.plugins.dotnet.api.sensor.AbstractRuleBasedDotNetSensor;
import org.sonar.plugins.dotnet.api.utils.FileFinder;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * Collects the ReSharper reporting into sonar.
 */
public abstract class ReSharperSensor extends AbstractRuleBasedDotNetSensor {

    private static final Logger LOG = LoggerFactory.getLogger(ReSharperSensor.class);

    private ProjectFileSystem fileSystem;
    private RulesProfile rulesProfile;
    private ReSharperResultParser resharperResultParser;
    private ReSharperConfiguration resharperConfiguration;

    @DependsUpon(DotNetConstants.CORE_PLUGIN_EXECUTED)
    public static class CSharpRegularReSharperSensor extends ReSharperSensor {
        public CSharpRegularReSharperSensor(ProjectFileSystem fileSystem, RulesProfile rulesProfile, ReSharperProfileExporter.CSharpRegularReSharperProfileExporter profileExporter,
                                            ReSharperResultParser resharperResultParser, ReSharperConfiguration configuration, MicrosoftWindowsEnvironment microsoftWindowsEnvironment) {
            super(fileSystem, rulesProfile, profileExporter, resharperResultParser, configuration, microsoftWindowsEnvironment);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] getSupportedLanguages() {
            return new String[] {"cs"};
        }
    }

    @DependsUpon(DotNetConstants.CORE_PLUGIN_EXECUTED)
    public static class VbNetRegularReSharperSensor extends ReSharperSensor {
        public VbNetRegularReSharperSensor(ProjectFileSystem fileSystem, RulesProfile rulesProfile, ReSharperProfileExporter.VbNetRegularReSharperProfileExporter profileExporter,
                                           ReSharperResultParser resharperResultParser, ReSharperConfiguration configuration, MicrosoftWindowsEnvironment microsoftWindowsEnvironment) {
            super(fileSystem, rulesProfile, profileExporter, resharperResultParser, configuration, microsoftWindowsEnvironment);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] getSupportedLanguages() {
            return new String[] {"vbnet"};
        }
    }


    /**
     * Constructs a {@link com.wrightfully.sonar.plugins.dotnet.resharper.ReSharperSensor}.
     *
     */
    protected ReSharperSensor(ProjectFileSystem fileSystem, RulesProfile rulesProfile,   ReSharperProfileExporter profileExporter,
                              ReSharperResultParser resharperResultParser, ReSharperConfiguration configuration, MicrosoftWindowsEnvironment microsoftWindowsEnvironment) {
        super(new DotNetConfiguration(configuration.getSettings()), rulesProfile, profileExporter, microsoftWindowsEnvironment, "ReSharper", configuration.getString(ReSharperConstants.MODE));
        this.fileSystem = fileSystem;
        this.rulesProfile = rulesProfile;

        this.resharperConfiguration = configuration;
        this.resharperResultParser = resharperResultParser;

    }

    /**
     * {@inheritDoc}
     */
    public void analyse(Project project, SensorContext context) {

        final Collection<File> reportFiles;
        String reportDefaultPath = getMicrosoftWindowsEnvironment().getWorkingDirectory() + "/" + ReSharperConstants.REPORT_FILENAME;
        String executionMode = getExecutionMode();

        if (MODE_REUSE_REPORT.equalsIgnoreCase(executionMode)) {
            String reportPath = resharperConfiguration.getString(ReSharperConstants.REPORTS_PATH_KEY);

            if (StringUtils.isEmpty(reportPath)) {
                reportPath = reportDefaultPath;
            }
            VisualStudioSolution vsSolution = getVSSolution();
            VisualStudioProject vsProject = getVSProject(project);
            reportFiles = FileFinder.findFiles(vsSolution, vsProject, reportPath);

            if (reportFiles.size() == 0){
                throw new SonarException("No ReSharper reports found. Make sure to set " + ReSharperConstants.REPORTS_PATH_KEY);
            }

            LOG.info("Reusing ReSharper reports: " + Joiner.on("; ").join(reportFiles));
        } else if (StringUtils.isEmpty(executionMode)) {
            try {
                ReSharperRunner runner = ReSharperRunner.create(resharperConfiguration.getString(ReSharperConstants.INSTALL_DIR_KEY));
                launchInspectCode(project, runner);
            } catch (ReSharperException e) {
                throw new SonarException("ReSharper execution failed.", e);
            }
            File projectDir = fileSystem.getBasedir();
            reportFiles = Collections.singleton(new File(projectDir, reportDefaultPath));
        }  else {
           throw new SonarException("Run Mode not supported: " + executionMode);
        }

        // and analyze results
        for (File reportFile : reportFiles) {
            analyseResults(reportFile);
        }
    }


    protected void launchInspectCode(Project project, ReSharperRunner runner) throws ReSharperException {
        VisualStudioSolution vsSolution = getVSSolution();
        VisualStudioProject vsProject = getVSProject(project);
        ReSharperCommandBuilder builder = runner.createCommandBuilder(vsSolution, vsProject);
        builder.setReportFile(new File(fileSystem.getSonarWorkingDirectory(), ReSharperConstants.REPORT_FILENAME));
        builder.setDotSettingsFilePath(resharperConfiguration.getString(ReSharperConstants.DOTSETTINGS_FILE_PATH));
        builder.setAdditionalParameters(resharperConfiguration.getString(ReSharperConstants.ADDITIONAL_CMD_ARGS));
        int timeout = resharperConfiguration.getInt(ReSharperConstants.TIMEOUT_MINUTES_KEY);
        runner.execute(builder, timeout);
    }

    private void analyseResults(File reportFile) throws SonarException {
        if (reportFile.exists()) {
            LOG.debug("ReSharper report found at location" + reportFile);
            resharperResultParser.parse(reportFile);
        } else {
            throw new SonarException("No ReSharper report found for path " + reportFile);
        }
    }

}
