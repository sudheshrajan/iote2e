#!/bin/bash
java -cp /tmp/data/ org.junit.runner.JUnitCore $4
./run-junit-tests-common.sh TestCommonLocalhost.properties TestLocalHandler.properties \
../jars/iote2e-tests-1.0.0.jar \
"com.pzybrick.iote2e.tests.local.TestLocalHandlerTempToFan com.pzybrick.iote2e.tests.local.TestLocalHandlerHumidityToMister  com.pzybrick.iote2e.tests.local.TestLocalHandlerLed"
