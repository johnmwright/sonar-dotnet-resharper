/*
 * .NET tools :: ReSharper Runner
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
package com.wrightfully.sonar.dotnet.tools.resharper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.command.CommandExecutor;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioProject;
import org.sonar.plugins.dotnet.api.microsoft.VisualStudioSolution;

import java.io.File;

/**
 * Class that runs the ReSharper inspectcode program.
 */
public final class ReSharperRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ReSharperRunner.class);

  private static final String RESHARPER_EXECUTABLE = "inspectcode.exe";

  private static final long MINUTES_TO_MILLISECONDS = 60000;

  private File resharperExecutable;

  private ReSharperRunner() {
  }

  /**
   * Creates a new {@link ReSharperRunner} object for the given executable file.
   * 
   * @param resharperPath
   *          the full path of the ReSharper install directory. For instance: "C:/Program Files/JetBrains/Command Line Tools".
   */
  public static ReSharperRunner create(String resharperPath) throws ReSharperException {
    ReSharperRunner runner = new ReSharperRunner();
    runner.resharperExecutable = new File(resharperPath, RESHARPER_EXECUTABLE);
    return runner;
  }

  /**
   * Creates a pre-configured {@link ReSharperCommandBuilder} that needs to be completed before running the
   * {@link #execute(ReSharperCommandBuilder, int)} method.
   * @param solution  the current VS solution
   * @param project   the VS project to analyse
   * 
   * @return the command to complete.
   */
  public ReSharperCommandBuilder createCommandBuilder(VisualStudioSolution solution, VisualStudioProject project) {
    ReSharperCommandBuilder builder = ReSharperCommandBuilder.createBuilder(solution, project);
    builder.setExecutable(resharperExecutable);
    return builder;
  }

  /**
   * Executes the given ReSharper command.
   * 
   * @param resharperCommandBuilder
   *          the resharperCommandBuilder
   * @param timeoutMinutes
   *          the timeout for the command
   * @throws ReSharperException
   *           if ReSharper fails to execute
   */
  public void execute(ReSharperCommandBuilder resharperCommandBuilder, int timeoutMinutes) throws ReSharperException {
    LOG.debug("Executing ReSharper program...");
    int exitCode = CommandExecutor.create().execute(resharperCommandBuilder.toCommand(), timeoutMinutes * MINUTES_TO_MILLISECONDS);
    if (exitCode != 0 ) {
      throw new ReSharperException("ReSharper execution failed with return code '" + exitCode
        + "'. Check ReSharper documentation for more information.");
    }
  }

}
