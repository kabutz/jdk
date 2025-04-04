/*
 * Copyright (c) 1999, 2025, Oracle and/or its affiliates. All rights reserved.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#include "ci/ciMethodType.hpp"
#include "ci/ciSignature.hpp"
#include "ci/ciStreams.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/signature.hpp"

// ciSignature
//
// This class represents the signature of a method.

// ------------------------------------------------------------------
// ciSignature::ciSignature
ciSignature::ciSignature(ciKlass* accessing_klass, const constantPoolHandle& cpool, ciSymbol* symbol)
  : _symbol(symbol), _accessing_klass(accessing_klass), _types(CURRENT_ENV->arena(), 8, 0, nullptr) {
  ASSERT_IN_VM;
  EXCEPTION_CONTEXT;
  assert(accessing_klass != nullptr, "need origin of access");

  ciEnv* env = CURRENT_ENV;

  int size = 0;
  ResourceMark rm(THREAD);
  for (SignatureStream ss(symbol->get_symbol()); !ss.is_done(); ss.next()) {
    // Process one element of the signature
    ciType* type = nullptr;
    if (ss.is_reference()) {
      ciSymbol* klass_name = env->get_symbol(ss.as_symbol());
      type = env->get_klass_by_name_impl(_accessing_klass, cpool, klass_name, false);
    } else {
      type = ciType::make(ss.type());
    }
    if (ss.at_return_type()) {
      // don't include return type in size calculation
      _return_type = type;
    } else {
      _types.append(type);
      size += type->size();
    }
  }
  _size = size;
}

// ------------------------------------------------------------------
// ciSignature::equals
//
// Compare this signature to another one.  Signatures with different
// accessing classes but with signature-types resolved to the same
// types are defined to be equal.
bool ciSignature::equals(ciSignature* that) {
  // Compare signature
  if (!this->as_symbol()->equals(that->as_symbol())) {
    return false;
  }
  // Compare all types of the arguments
  if (_types.length() != that->_types.length()) {
    return false;
  }
  for (int i = 0; i < _types.length(); i++) {
    if (this->type_at(i) != that->type_at(i)) {
      return false;
    }
  }
  // Compare the return type
  if (this->return_type() != that->return_type()) {
    return false;
  }
  return true;
}

// ------------------------------------------------------------------
// ciSignature::has_unloaded_classes
//
// Reports if there are any unloaded classes present in the signature.
// Each ciSignature when instantiated is resolved against some accessing class
// and the resolved classes aren't required to be local, but can be revealed
// through loader constraints.
bool ciSignature::has_unloaded_classes() {
  for (ciSignatureStream str(this); !str.is_done(); str.next()) {
    if (!str.type()->is_loaded()) {
      return true;
    }
  }
  return false;
}

// ------------------------------------------------------------------
// ciSignature::print_signature
void ciSignature::print_signature() {
  _symbol->print_symbol();
}

// ------------------------------------------------------------------
// ciSignature::print
void ciSignature::print() {
  tty->print("<ciSignature symbol=");
  print_signature();
 tty->print(" accessing_klass=");
  _accessing_klass->print();
  tty->print(" address=" INTPTR_FORMAT ">", p2i((address)this));
}
