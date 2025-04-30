# Modbus Schema Toolkit
This is the Modbus Schema Toolkit I ([Niels Basjes](https://niels.basjes.nl)) created that makes retrieving data from Modbus based devices a lot easier.

I have split this into 3 projects:
- [Modbus Schema](https://github.com/nielsbasjes/modbus-schema):
  - A toolkit and schema definition
  - [![License](https://img.shields.io/:license-apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html) [![Github actions Build status](https://img.shields.io/github/actions/workflow/status/nielsbasjes/modbus-schema/build.yml?branch=main&label=main%20branch)](https://github.com/nielsbasjes/modbus-schema/actions) [![Maven Central](https://img.shields.io/maven-central/v/nl.basjes.modbus/modbus-schema-parent.svg?label=Maven%20Central)](https://central.sonatype.com/namespace/nl.basjes.modbus)
    [![GitHub stars](https://img.shields.io/github/stars/nielsbasjes/modbus-schema?label=GitHub%20stars)](https://github.com/nielsbasjes/modbus-schema/stargazers)

- [Modbus Devices](https://github.com/nielsbasjes/modbus-devices):
  - The actual schemas of a few devices.
  - [![License](https://img.shields.io/:license-apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html) [![Github actions Build status](https://img.shields.io/github/actions/workflow/status/nielsbasjes/modbus-devices/build.yml?branch=main&label=main%20branch)](https://github.com/nielsbasjes/modbus-devices/actions) [![Maven Central](https://img.shields.io/maven-central/v/nl.basjes.modbus.devices/modbus-device-schemas-parent.svg?label=Maven%20Central)](https://central.sonatype.com/namespace/nl.basjes.modbus.devices)
    [![GitHub stars](https://img.shields.io/github/stars/nielsbasjes/modbus-devices?label=GitHub%20stars)](https://github.com/nielsbasjes/modbus-devices/stargazers)

- [SunSpec Device](https://github.com/nielsbasjes/sunspec-device):
  - Generate the Modbus Schema for the specific SunSpec you have
  - [![License](https://img.shields.io/:license-apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html) [![Github actions Build status](https://img.shields.io/github/actions/workflow/status/nielsbasjes/sunspec-device/build.yml?branch=main&label=main%20branch)](https://github.com/nielsbasjes/sunspec-device/actions) [![Maven Central](https://img.shields.io/maven-central/v/nl.basjes.modbus.devices/sunspec-device-parent.svg?label=Maven%20Central)](https://central.sonatype.com/namespace/nl.basjes.sunspec)
    [![GitHub stars](https://img.shields.io/github/stars/nielsbasjes/sunspec-device?label=GitHub%20stars)](https://github.com/nielsbasjes/sunspec-device/stargazers)

The documentation can be found here https://modbus.basjes.nl/

All of this was created by [Niels Basjes](https://niels.basjes.nl/).

## The problem
I have multiple devices at home that are capable of exposing metrics using the modbus protocol.
My solar inverter reports how much power it is delivering, my heatpump reports the temperature of the hot water and an electricity meter I have exposes how much power is being used.

The key problem is that Modbus is a binary protocol and the modbus itself does not give any meaning to the bits you can retrieve.

To add meaning to these bits the historical reality is that you would get a PDF from the manufacturer and there you could read in a table how the bits need to be interpreted.

So for every device everyone who want to read the data needs to reinvent the wheel over and over again.

I've spent too much time debugging nasty problems regarding reading Modbus and the fact that the most common notation is a 1-off compared to the read address on the wire.

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

Things I will **NOT** change/fix:
- It is READ ONLY. So NO writing. I will not change that because I consider that too much of a risk.

Things I intend to fix:
- Better website & more documentation.
- Only Registers right now. So no Coils and/or Discrete Inputs yet.
- If a register returns an error then the system should mark that as unreadable and avoid reading it in the future.

Working on:
- SunSpec which is a completely separate beast.

## Overall status
Works on my machine. Usable for experiments.

## I spoke about this

In 2024 the company I work for ([bol.com](https://partner.bol.com/click/click?p=2&t=url&s=2483&f=TXL&url=https%3A%2F%2Fwww.bol.com%2Fnl%2Fnl%2F&name=Modbus)) held an internal IT Conference where I gave a presentation about "[The dark side of Green Energy APIs](https://youtu.be/CHVktAbJbHc)" which is all about modbus and this project.

<iframe width="600" height="360" src="https://www.youtube.com/embed/CHVktAbJbHc" title="The dark side of Green Energy APIs" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>

# License
I'm publishing this under the Apache 2.0 license because I believe this can be part of making this planet a bit more in control of the energy consumption.
I also believe that making this open for all to use is the best way to achieve this.

But do not underestimate how much work went into this. From my first attempts to releasing the first version took be about 5 years of spending my spare time.

So what I want to see in return is a little bit of gratitude from the people who use this.
If you are a home user/hobbyist/small business then a simple star on the projects you use is enough for me. Seeing that people use and like the things I create is what I'm doing this for.
What also really helps is bug reports, dumps from real devices I do not have and discussions on things you think can be done better.

Despite there not being any obligation (because of the Apache 2.0 license); If you are a big corporation where my code really adds value to the products you make/sell then I would really appreciate it if you could do a small sponsor thing. Buy me lunch (€10), Buy me a game (€100) or what ever you think is the right way to say thank you for the work I have done.

[![If this project has business value for you then don't hesitate to support me with a small donation.](https://img.shields.io/badge/Sponsor%20me-via%20Github-red.svg)](https://github.com/sponsors/nielsbasjes)

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
