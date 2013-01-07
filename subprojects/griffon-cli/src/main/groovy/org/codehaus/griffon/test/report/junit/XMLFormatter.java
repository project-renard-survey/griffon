/*
 * Copyright 2009-2013 the original author or authors.
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

package org.codehaus.griffon.test.report.junit;

import junit.framework.Test;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.codehaus.griffon.test.support.TestStacktraceSanitizer;

import java.io.*;

/**
 * JUnit XML formatter that sanitises the stack traces generated by
 * tests.
 */
public class XMLFormatter extends XMLJUnitResultFormatter {
    public XMLFormatter(File file) {
        try {
            super.setOutput(new BufferedOutputStream(new FileOutputStream(file)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void setOutput(OutputStream out) {
        throw new IllegalStateException("This should not be called");
    }

    public void addFailure(Test test, Throwable throwable) {
        TestStacktraceSanitizer.sanitize(throwable);
        super.addFailure(test, (Throwable) throwable);
    }

    public void addError(Test test, Throwable throwable) {
        TestStacktraceSanitizer.sanitize(throwable);
        super.addError(test, throwable);
    }
}
