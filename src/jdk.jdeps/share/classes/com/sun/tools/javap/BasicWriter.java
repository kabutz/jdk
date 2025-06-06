/*
 * Copyright (c) 2007, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.javap;

import java.io.PrintWriter;
import java.lang.classfile.AccessFlags;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.ClassFileFormatVersion;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/*
 *  A writer similar to a PrintWriter but which does not hide exceptions.
 *  The standard print calls are line-buffered; report calls write messages directly.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class BasicWriter {

    protected BasicWriter(Context context) {
        lineWriter = LineWriter.instance(context);
        out = context.get(PrintWriter.class);
        messages = context.get(Messages.class);
        if (messages == null)
            throw new AssertionError();
    }

    protected Set<AccessFlag> flagsReportUnknown(AccessFlags flags, ClassFileFormatVersion cffv) {
        return maskToAccessFlagsReportUnknown(flags.flagsMask(), flags.location(), cffv);
    }

    protected Set<AccessFlag> maskToAccessFlagsReportUnknown(int mask, AccessFlag.Location location, ClassFileFormatVersion cffv) {
        // TODO pass cffv to maskToAccessFlags
        try {
            return AccessFlag.maskToAccessFlags(mask, location);
        } catch (IllegalArgumentException ex) {
            mask &= location.flagsMask(cffv);
            report("Access Flags: " + ex.getMessage());
            return AccessFlag.maskToAccessFlags(mask, location);
        }
    }

    protected void print(String s) {
        lineWriter.print(s);
    }

    protected void print(Object o) {
        lineWriter.print(o == null ? null : o.toString());
    }

    protected void print(Supplier<Object> safeguardedCode) {
        try {
            print(safeguardedCode.get());
        } catch (IllegalArgumentException e) {
            print(report(e));
        }
    }

    protected void println() {
        lineWriter.println();
    }

    protected void println(String s) {
        lineWriter.print(s);
        lineWriter.println();
    }

    protected void println(Object o) {
        lineWriter.print(o == null ? null : o.toString());
        lineWriter.println();
    }

    protected void println(Supplier<Object> safeguardedCode) {
        print(safeguardedCode);
        lineWriter.println();
    }

    protected void indent(int delta) {
        lineWriter.indent(delta);
    }

    protected void tab() {
        lineWriter.tab();
    }

    protected void setPendingNewline(boolean b) {
        lineWriter.pendingNewline = b;
    }

    protected String report(Exception e) {
        out.println("Error: " + e.getMessage()); // i18n?
        errorReported = true;
        return "???";
    }

    protected String report(String msg) {
        out.println("Error: " + msg); // i18n?
        errorReported = true;
        return "???";
    }

    protected String space(int w) {
        if (w < spaces.length && spaces[w] != null)
            return spaces[w];

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w; i++)
            sb.append(" ");

        String s = sb.toString();
        if (w < spaces.length)
            spaces[w] = s;

        return s;
    }

    private String[] spaces = new String[80];

    private LineWriter lineWriter;
    private PrintWriter out;
    protected Messages messages;
    protected boolean errorReported;

    private static class LineWriter {
        static LineWriter instance(Context context) {
            LineWriter instance = context.get(LineWriter.class);
            if (instance == null)
                instance = new LineWriter(context);
            return instance;
        }

        protected LineWriter(Context context) {
            context.put(LineWriter.class, this);
            Options options = Options.instance(context);
            indentWidth = options.indentWidth;
            tabColumn = options.tabColumn;
            out = context.get(PrintWriter.class);
            buffer = new StringBuilder();
        }

        protected void print(String s) {
            if (pendingNewline) {
                println();
                pendingNewline = false;
            }
            if (s == null)
                s = "null";
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case ' ':
                        pendingSpaces++;
                        break;

                    case '\n':
                        println();
                        break;

                    default:
                        if (buffer.length() == 0)
                            indent();
                        if (pendingSpaces > 0) {
                            for (int sp = 0; sp < pendingSpaces; sp++)
                                buffer.append(' ');
                            pendingSpaces = 0;
                        }
                        buffer.append(c);
                }
            }

        }

        protected void println() {
            // ignore/discard pending spaces
            pendingSpaces = 0;
            out.println(buffer);
            buffer.setLength(0);
        }

        protected void indent(int delta) {
            indentCount += delta;
        }

        protected void tab() {
            int col = indentCount * indentWidth + tabColumn;
            pendingSpaces += (col <= buffer.length() ? 1 : col - buffer.length());
        }

        private void indent() {
            pendingSpaces += (indentCount * indentWidth);
        }

        private final PrintWriter out;
        private final StringBuilder buffer;
        private int indentCount;
        private final int indentWidth;
        private final int tabColumn;
        private boolean pendingNewline;
        private int pendingSpaces;
    }
}

