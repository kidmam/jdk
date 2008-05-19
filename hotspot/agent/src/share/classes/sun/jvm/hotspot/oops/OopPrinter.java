/*
 * Copyright 2000-2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 */

package sun.jvm.hotspot.oops;

import java.io.*;

public class OopPrinter implements OopVisitor {
  public OopPrinter(PrintStream tty) {
    this.tty = tty;
  }

  PrintStream tty;

  public void prologue() {
    Oop.printOopValueOn(getObj(), tty);
    tty.println(" (object size = " + getObj().getObjectSize() + ")");
  }

  public void epilogue() {
    tty.println();
  }


  private Oop obj;
  public void setObj(Oop obj) { this.obj = obj; }
  public Oop getObj() { return obj; }


  private void printField(Field field) {
    field.printOn(tty);
  }

  public void doOop(OopField field, boolean isVMField) {
    printField(field);
    Oop.printOopValueOn(field.getValue(getObj()), tty);
    tty.println();
  }

  public void doOop(NarrowOopField field, boolean isVMField) {
    printField(field);
    Oop.printOopValueOn(field.getValue(getObj()), tty);
    tty.println();
  }

  public void doChar(CharField field, boolean isVMField) {
    printField(field);
    char c = field.getValue(getObj());
    // Fix me: not yet complete
    if (Character.isLetterOrDigit(c)) tty.println(c);
    else tty.println((int)c);
  }
  public void doByte(ByteField field, boolean isVMField) {
    printField(field);
    tty.println(field.getValue(getObj()));
  }
  public void doBoolean(BooleanField field, boolean isVMField) {
    printField(field);
    tty.println(field.getValue(getObj()));
  }
  public void doShort(ShortField field, boolean isVMField) {
    printField(field);
    tty.println(field.getValue(getObj()));
  }
  public void doInt(IntField field, boolean isVMField) {
    printField(field);
    tty.println(field.getValue(getObj()));
  }
  public void doLong(LongField field, boolean isVMField) {
    printField(field);
    tty.println(field.getValue(getObj()));
  }
  public void doFloat(FloatField field, boolean isVMField) {
    printField(field);
    tty.println(field.getValue(getObj()));
  }
  public void doDouble(DoubleField field, boolean isVMField) {
    printField(field);
    tty.println(field.getValue(getObj()));
  }
  public void doCInt(CIntField field, boolean isVMField) {
    printField(field);
    tty.println(field.getValue(getObj()));
  }
}
