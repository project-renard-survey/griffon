/*
 * Copyright 2011-2013 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.griffon.cli.parsing;

import griffon.util.Environment;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

import java.util.HashMap;
import java.util.Map;

/**
 * Command line parser that parses arguments to the command line. Written as a
 * replacement for Commons CLI because it doesn't support unknown arguments and
 * requires all arguments to be declared up front.
 * <p/>
 * It also doesn't support command options with hyphens. This class gets around those problems.
 *
 * @author Graeme Rocher (Grails 2.0)
 */
public class CommandLineParser {
    static Map<String, String> ENV_ARGS = new HashMap<String, String>();
    static Map<String, String> DEFAULT_ENVS = new HashMap<String, String>();
    private static final String DEFAULT_PADDING = "        ";

    static {
        ENV_ARGS.put("dev", Environment.DEVELOPMENT.getName());
        ENV_ARGS.put("prod", Environment.PRODUCTION.getName());
        ENV_ARGS.put("test", Environment.TEST.getName());
        DEFAULT_ENVS.put("Console", Environment.TEST.getName());
        DEFAULT_ENVS.put("Shell", Environment.TEST.getName());
        DEFAULT_ENVS.put("Package", Environment.PRODUCTION.getName());
        DEFAULT_ENVS.put("TestApp", Environment.TEST.getName());
    }

    private Map<String, Option> declaredOptions = new HashMap<String, Option>();
    private int longestOptionNameLength = 0;

    public static String getExtendedEnvironmnentName(String env) {
        return ENV_ARGS.containsKey(env) ? ENV_ARGS.get(env) : env;
    }

    public static String getDefaultEnvironmentForScript(String scriptName) {
        return DEFAULT_ENVS.get(scriptName);
    }

    /**
     * Adds a declared option
     *
     * @param name        The name of the option
     * @param description The description
     */
    public void addOption(String name, String description) {
        int length = name.length();
        if (length > longestOptionNameLength) {
            longestOptionNameLength = length;
        }
        declaredOptions.put(name, new Option(name, description));
    }

    /**
     * Parses a string of all the command line options converting them into an array of arguments to pass to #parse(String..args)
     *
     * @param string The string
     * @return The command line
     */
    public CommandLine parseString(String string) {
        // Steal ants implementation for argument splitting. Handles quoted arguments with " or '.
        // Doesn't handle escape sequences with \
        try {
            return parse(Commandline.translateCommandline(string));
        } catch (BuildException e) {
            throw new ParseException(e); //Rethrow as an error that clients can expect.
        }
    }

    /**
     * Parses a string of all the command line options converting them into an array of arguments to pass to #parse(String..args)
     *
     * @param commandName The command name
     * @param args        The string
     * @return The command line
     */
    public CommandLine parseString(String commandName, String args) {
        // Steal ants implementation for argument splitting. Handles quoted arguments with " or '.
        // Doesn't handle escape sequences with \
        try {
            String[] argArray = Commandline.translateCommandline(args);
            DefaultCommandLine cl = createCommandLine();
            cl.setCommandName(commandName);
            parseInternal(cl, argArray, false);
            return cl;
        } catch (BuildException e) {
            throw new ParseException(e); //Rethrow as an error that clients can expect.
        }
    }

    /**
     * Parses the given list of command line arguments. Arguments starting with -D become system properties,
     * arguments starting with -- or - become either declared or undeclared options. All other arguments are
     * put into a list of remaining arguments
     *
     * @param args The arguments
     * @return The command line state
     */
    public CommandLine parse(String... args) {
        DefaultCommandLine cl = createCommandLine();
        parseInternal(cl, args, true);
        return cl;
    }

    private void parseInternal(DefaultCommandLine cl, String[] args, boolean firstArgumentIsCommand) {
        for (String arg : args) {
            if (arg == null) continue;
            String trimmed = arg.trim();
            if (trimmed != null && trimmed.length() > 0) {
                if (trimmed.charAt(0) == '-') {
                    processOption(cl, trimmed);
                } else {
                    if (firstArgumentIsCommand && ENV_ARGS.containsKey(trimmed)) {
                        cl.setEnvironment(ENV_ARGS.get(trimmed));
                    } else {
                        if (firstArgumentIsCommand) {
                            cl.setCommandName(trimmed);
                            firstArgumentIsCommand = false;
                        } else {
                            cl.addRemainingArg(trimmed);
                        }
                    }
                }
            }
        }
    }

    public String getHelpMessage() {
        String ls = System.getProperty("line.separator");
        String usageMessage = "usage: griffon [options] [command]";
        StringBuilder sb = new StringBuilder(usageMessage);
        sb.append(ls);
        for (Option option : declaredOptions.values()) {
            String name = option.getName();
            int extraPadding = longestOptionNameLength - name.length();
            sb.append(" -").append(name);
            for (int i = 0; i < extraPadding; i++) {
                sb.append(' ');
            }
            sb.append(DEFAULT_PADDING).append(option.getDescription()).append(ls);
        }

        return sb.toString();
    }

    protected DefaultCommandLine createCommandLine() {
        return new DefaultCommandLine();
    }

    protected void processOption(DefaultCommandLine cl, String arg) {
        if (arg.length() < 2) {
            return;
        }

        if (arg.charAt(1) == 'D' && arg.contains("=")) {
            processSystemArg(cl, arg);
            return;
        }

        arg = (arg.charAt(1) == '-' ? arg.substring(2, arg.length()) : arg.substring(1, arg.length())).trim();

        if (arg.contains("=")) {
            String[] split = arg.split("=");
            String name = split[0].trim();
            validateOptionName(name);
            String value = arg.substring(name.length() + 1);
            if (declaredOptions.containsKey(name)) {
                cl.addDeclaredOption(name, declaredOptions.get(name), value);
            } else {
                cl.addUndeclaredOption(name, value);
            }
            return;
        }

        validateOptionName(arg);
        if (declaredOptions.containsKey(arg)) {
            cl.addDeclaredOption(arg, declaredOptions.get(arg));
        } else {
            cl.addUndeclaredOption(arg);
        }
    }

    private void validateOptionName(String name) {
        if (name.contains(" ")) throw new ParseException("Invalid argument: " + name);
    }

    protected void processSystemArg(DefaultCommandLine cl, String arg) {
        int i = arg.indexOf("=");
        String name = arg.substring(2, i);
        String value = arg.substring(i + 1, arg.length());
        cl.addSystemProperty(name, value);
    }
}
