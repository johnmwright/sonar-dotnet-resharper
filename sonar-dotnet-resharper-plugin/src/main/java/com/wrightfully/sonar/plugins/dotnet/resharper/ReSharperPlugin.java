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
import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperProfileImporter;
import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperSonarWayProfileCSharp;
import com.wrightfully.sonar.plugins.dotnet.resharper.profiles.ReSharperSonarWayProfileVbNet;
import org.sonar.api.*;
import org.sonar.plugins.dotnet.api.sensor.AbstractDotNetSensor;

import java.util.ArrayList;
import java.util.List;

/**
 * Main class of the ReSharper plugin.
 */
@Properties({
        @Property(key = ReSharperConstants.MODE, defaultValue = "", name = "ReSharper activation mode",
                description = "Possible values : empty (means active), 'skip' and 'reuseReport'.", global = false, project = false,
                type = PropertyType.SINGLE_SELECT_LIST, options = {AbstractDotNetSensor.MODE_SKIP, AbstractDotNetSensor.MODE_REUSE_REPORT}),
        @Property(key = ReSharperConstants.REPORTS_PATH_KEY, defaultValue = "", name = "Path of the ReSharper report file(s)",
                description = "Path of the ReSharper report file(s) used when reuse report mode is activated. "
                        + "This can be an absolute path, or a path relative to each project base directory.", global = false, project = false),
        @Property(key = ReSharperConstants.INSTALL_DIR_KEY, defaultValue = ReSharperConstants.INSTALL_DIR_DEFVALUE, name = "ReSharper Command Line Tools install directory",
                description = "Absolute path of the ReSharper Command Line Tools installation folder.", global = true, project = false),
        @Property(key = ReSharperConstants.DOTSETTINGS_FILE_PATH, defaultValue = "", name = "ReSharper dotSettings file",
                description = "Absolute path of the ReSharper dotSettings file to use during in-process analysis.", global = false, project = false),
        @Property(key = ReSharperConstants.ADDITIONAL_CMD_ARGS, defaultValue = "", name = "Additional ReSharper commandline arguments",
                description = "Additional commandline arguments to use during in-process analysis.", global = false, project = false),
        @Property(key = ReSharperConstants.TIMEOUT_MINUTES_KEY, defaultValue = ReSharperConstants.TIMEOUT_MINUTES_DEFVALUE + "",
                name = "ReSharper program timeout", description = "Maximum number of minutes before the ReSharper program will be stopped.", global = true,
                project = true, type = PropertyType.INTEGER),
        @Property(key = ReSharperConstants.INCLUDE_ALL_FILES, defaultValue = "true",
                name = "ReSharper file inclusion mode", description = "Determines if violations are reported on any file (ignores filters and unsupported file types) or only those supported by the dotNet core plugin.", global = false,
                project = false, type = PropertyType.BOOLEAN)
})
public class ReSharperPlugin extends SonarPlugin {

    /**
     * {@inheritDoc}
     */
    public List<Class<? extends Extension>> getExtensions() {
        List<Class<? extends Extension>> list = new ArrayList<Class<? extends Extension>>();

        list.add(ReSharperConfiguration.class);


        // sensors
        list.add(ReSharperSensor.CSharpRegularReSharperSensor.class);
        list.add(ReSharperSensor.VbNetRegularReSharperSensor.class);

        // Rules and profiles
        list.add(ReSharperRuleRepositoryProvider.class);
        list.add(ReSharperProfileExporter.CSharpRegularReSharperProfileExporter.class);
        list.add(ReSharperProfileExporter.VbNetRegularReSharperProfileExporter.class);
        list.add(ReSharperProfileImporter.CSharpRegularReSharperProfileImporter.class);
        list.add(ReSharperProfileImporter.VbNetRegularReSharperProfileImporter.class);
        list.add(ReSharperSonarWayProfileCSharp.class);
        list.add(ReSharperSonarWayProfileVbNet.class);

        // Running ReSharper
        list.add(ReSharperResultParser.class);

        return list;
    }
}
