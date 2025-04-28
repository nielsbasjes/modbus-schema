/*
 * Copyright (C) 2019-2025 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

parser grammar FieldExpressionsParser;

options { tokenVocab=FieldExpressionsLexer; }

// ===============================================================
// SIMPLIFICATION: ONLY DO STRING, number and LONG expressions

// An expression can only result in one of these
expression
    : number     EOF
    | string     EOF
    | stringList EOF
    ;

singleRegister
    : REGISTER
    | HEXLIKEREGISTER
    | NUMBER56
    | LONG
    ;

registerlist
    : singleRegister ( COMMA singleRegister ) *                             #registers          // "1x0001, 1x0002,1x0003,1x0004"
    | startRegister=singleRegister HASH count=LONG                          #registerCount      // "1x0001 # 4" Starting at 1x0001 do 4 registers
    | startRegister=singleRegister DOTDOT lastRegister=singleRegister       #registerRange      // "1x0001.. 1x0004" range both start and end inclusive!
    | SWAPENDIAN    BRACEOPEN registers=registerlist BRACECLOSE             #registerSwapEndian // Reverse the ordering of the retrieved bits
    | SWAPBYTES     BRACEOPEN registers=registerlist BRACECLOSE             #registerSwapBytes  // Reverse the ordering of the retrieved bytes
    | DOUBLEQUOTE constantHexString DOUBLEQUOTE                             #registerValues     // A hardcoded list of register values (between " to separate them from the singleRegister coils)
    ;

constantHexString
    : ( HEXLIKEREGISTER | HEXVALUE )+
    ;

string
    : UTF8            BRACEOPEN registers=registerlist                                                       BRACECLOSE #stringUtf8
    | HEXSTRING       BRACEOPEN registers=registerlist                                                       BRACECLOSE #stringHex
    | EUI48           BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )*                         BRACECLOSE #stringEui48
    | IPv4ADDR        BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )*                         BRACECLOSE #stringIPv4Addr
    | IPv6ADDR        BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )*                         BRACECLOSE #stringIPv6Addr
    | ENUM            BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )*  ( SEMICOLON mapping )+ BRACECLOSE #stringEnum
    | CONCAT          BRACEOPEN stringFragments        ( COMMA stringFragments    )*                         BRACECLOSE #stringConcat
    | STRING                                                                                                            #stringConstant
    ;

stringFragments
    : string                                                                                                            #stringString
    | FIELDNAME                                                                                                         #stringField
    | number                                                                                                            #stringNumber
    ;

stringList
    : BITSET          BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )*  ( SEMICOLON mapping )+ BRACECLOSE #stringListBitSet
    ;

notImplemented
    : ( HEXLIKEREGISTER | HEXVALUE )+
    ;

mapping
    : key=LONG ARROW value=STRING
    ;

number
    // Convert registers to value
    : INT16  BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE            #loadInt16
    | INT32  BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE            #loadInt32
    | INT64  BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE            #loadInt64
    | UINT16 BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE            #loadUInt16
    | UINT32 BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE            #loadUInt32
    | UINT64 BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE            #loadUInt64
    // NOTE: IEEE754-32/64 have a special value for NaN "Not a Number" which SHOULD be used to indicate "Not Implemented"
    | IEEE754_32      BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE   #loadIeee754_32
    | IEEE754_64      BRACEOPEN registers=registerlist ( SEMICOLON notImplemented )* BRACECLOSE   #loadIeee754_64

    // Constants
    | MINUS? (LONG|NUMBER56)                                                                      #longConstant
    | MINUS? (DOUBLE)                                                                             #doubleConstant

    // Base operation
    | BRACEOPEN number BRACECLOSE                                                                 #extraBraces

    // Other fields
    // Fields are only used when a calculation is needed (like in SunSpec) and
    // thus it is assumed to always be a number (which is true in all known cases where this is needed)
    | FIELDNAME                                                                                   #numberField

    // Convert registers to value
    | base=number     POWER              exponent=number                                          #power
    | left=number     (MULTIPLY|DIVIDE)  right=number                                             #multiplyDivide
    |           left=number               BRACEOPEN middle=number BRACECLOSE                      #implicitMultiply
    | BRACEOPEN left=number  BRACECLOSE   BRACEOPEN middle=number BRACECLOSE                      #implicitMultiply
//    Next two have ambiguity with negative numbers at the right.  "(3)-2" -->  "(3-2)" OR "(3 * -2)"
//    So anything in that form will NOT be seen as a multiplication but as a subtraction!
//    |         left=number               BRACEOPEN middle=number BRACECLOSE right=number         #implicitMultiply
//    |                                   BRACEOPEN middle=number BRACECLOSE right=number         #implicitMultiply
    | left=number     (ADD|MINUS)        right=number                                             #addSubtract
    ;
