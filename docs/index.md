# Modbus Schema Toolkit

## The problem
I have multiple devices at home that are capable of exposing metrics using the modbus protocol.
My solar inverter reports how much power it is delivering, my heatpump reports the temperature of the hot water and an electricity meter I have exposes how much power is being used.

The key problem is that Modbus is a binary protocol and the modbus itself does not give any meaning to the bits you can retrieve.

To add meaning to these bits the historical reality is that you would get a PDF from the manufacturer and there you could read in a table how the bits need to be interpreted.

So for every device everyone who want to read the data needs to reinvent the wheel over and over again.

I've spent too much time debugging nasty problems regarding reading Modbus and the fact that the most common notation is a `1 off` compared to the read address on the wire.

## What is this?
A library that is intended to make it much easier and more reliable and more reusable to get data from Modbus devices.

The overall goals
1. creating a generic way of defining the extraction of data from a modbus device that maps the raw registers into meaning full values.
2. doing it in such a way that a code generator for any (common) programming language can be created that generates the code needed to read the data from the device and actually execute the transformation into the meaning full fields.
3. actually do this for the devices I have at home and generate the Kotlin/Java code needed to really do this.

So essentially the intent of this project is to create a machine-readable standard schema/format specification for modbus devices that can be used to generate code.

Intended effects:
- The schema for a specific device only needs to be written ONCE.
- The code generator + runtime for a specific language only needs to be written ONCE.
- For all devices it is now trivial to generate code that maps the registers into usable values.
- A new device is immediately available in all programming languages.
- A new language immediately can provide tooling for all defined devices.

## Current features
- A schema definition
  - which allows very flexible expressions (Byte ordering, Integers, Floats, Strings, Enums, Bitmaps, PEMDAS Math, String concat, etc.).
  - has the option to include tests
- A generic API towards the actual Modbus implementation (I implemented Apache Plc4J and J2Mod)
- A retrieval system that optimizes the retrieval of the needed registers to reduce the number of Modbus calls.
- Code generation of Kotlin and Java

## Known limitations/Problems/Bugs

Things I will `NOT` change/fix:
- It is READ ONLY. So NO writing. I will not change that because I consider that too much of a risk.

Things I intend to fix:
- Only Registers right now. So no Coils and/or Discrete Inputs yet.
- If a register returns an error then the system should mark that as unreadable and avoid reading it in the future.

Working on:
- SunSpec which is a completely separate beast.

## Overall status
Works on my machine. Usable for experiments.

## Donations
If this project has business value for you then don't hesitate to support me with a small donation.

[![Donations via PayPal](https://img.shields.io/badge/Donations-via%20Paypal-blue.svg)](https://www.paypal.me/nielsbasjes)

---

## License
    Modbus Schema Toolkit
    Copyright (C) 2019-2025 Niels Basjes

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
