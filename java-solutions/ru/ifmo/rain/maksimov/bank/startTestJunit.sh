#!/bin/bash

cd ../../../../../out/production/java-solutions

java -jar ../../../lib/junit-platform-console-standalone-1.5.2.jar -cp . -c ru.ifmo.rain.maksimov.bank.BankTests
