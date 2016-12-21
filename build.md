To Perform A Release
====================
1) mvn release:prepare
2) mvn release:perform

or

1) mvn release:prepare release:perform

To Perform a Local Build
========================
1) mvn -Dmaven.test.skip=true clean install -U

(-U is to force update of dependencies)


To Perform a SNAPSHOT Release
=============================
1) mvn deploy


Version Number Examples
=======================
Release Builds - 3.3.0 3.3.0-beta-1
Snapshot Builds - 3.3.0-SNAPSHOT 3.3.0-beta-1-SNAPSHOT
 

