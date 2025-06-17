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

lexer grammar FieldExpressionsLexer;

options { caseInsensitive = true; }

// ===============================================================

DOUBLEQUOTE     : '"'                           ;
DOUBLE          : [0-9]+ '.' [0-9]+             ;
NUMBER56        : [0-9][0-9][0-9][0-9][0-9]([0-9])? ; // A 5 or 6 digit number
LONG            : ([0-9] | [1-9][0-9]+)         ;
BRACEOPEN       : '('                           ;
BRACECLOSE      : ')'                           ;
SEMICOLON       : ';'                           ;
COMMA           : ','                           ;
DOTDOT          : '..'                          ;
HASH            : '#'                           ;
ARROW           : '->'                          ;

SPACE           : (' '|'\t')+   -> skip;

POWER           : '^'                           ;
MULTIPLY        : '*'                           ;
DIVIDE          : '/'                           ;
ADD             : '+'                           ;
MINUS           : '-'                           ;

// Functions to manipulate the 2 bytes on a per register basis !!
// swapendian(register) converts between little and big endian
SWAPENDIAN      : 'swapendian' ; // Reverse 16 bits: 0xABCD ( 10101011 11001101 ) into 0xB3D5 ( 10110011 11010101 )
SWAPBYTES       : 'swapbytes'  ; // Reverse 2 bytes: 0xABCD into 0xCDAB

// The register to value function names
UTF8            : 'utf8'                        ;
HEXSTRING       : 'hexstring'                   ;
CONCAT          : 'concat'                      ;
EUI48           : 'eui48'                       ;
ENUM            : 'enum'                        ;
BITSET          : 'bitset'                      ;
BITSETBIT       : 'bitsetbit'                   ;
BOOLEAN         : 'boolean'                     ;
IEEE754_32      : 'ieee754_32'                  ;
IEEE754_64      : 'ieee754_64'                  ;
INT16           : 'int16'                       ;
INT32           : 'int32'                       ;
INT64           : 'int64'                       ;
UINT16          : 'uint16'                      ;
UINT32          : 'uint32'                      ;
UINT64          : 'uint64'                      ;
IPv4ADDR        : 'ipv4addr'                    ;
IPv6ADDR        : 'ipv6addr'                    ;

TRUE            : 'true' ;
FALSE           : 'false' ;

HEXLIKEREGISTER
    : '0x' [0-9][0-9][0-9][0-9]
    ;

ADDRESS
    : (
          'coil:'               | 'c:'  | '0x' |
          'discrete-input:'     | 'di:' | '1x' |
          'input-register:'     | 'ir:' | '3x' |
          'holding-register:'   | 'hr:' | '4x'
      ) [0-9]([0-9]([0-9]([0-9]([0-9]([0-9]([0-9]([0-9]([0-9]([0-9])?)?)?)?)?)?)?)?)?
    ;

HEXVALUE
    : '0x'[0-9a-f][0-9a-f][0-9a-f][0-9a-f]
    ;

FIELDNAME
    : [a-z]([a-z0-9_ ]*[a-z0-9_]+)?
    ;

STRING_START
    : '\'' -> channel(HIDDEN), pushMode(STRING_MODE)
    ;

mode STRING_MODE;
    STRING_CLOSE
        : '\'' -> channel(HIDDEN), type(STRING_START), popMode
        ;
    STRING
        : [a-z0-9~!@#$%^&*()_+={}|[\]\\:;"<,>.?/ -]+
        ;

