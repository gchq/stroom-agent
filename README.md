# Stroom Agent

_Stroom Agent_ is a simple java program that can be used for pulling data (such as log files) from remote hosts and forwarding it to _Stroom_.

## Stroom agent implementations

_Stroom Agent_ can be run from the command line (or from cron).  Based on the configuration you provide it can:

* Read log/audit files from a remote server (over ssh)

* Read log/audit information from a database (given a JDBC driver and some SQL to run)

* Stores these files in a local Stroom Zip Repository (aka _Stroom Proxy_ format)

* Forwards the buffered files onto a Stroom server

## Source Code

Stroom Agent uses _stroom-agent.jar_, _stroom-agent-util.jar_ and a number of standard jar libaries.

These are deployed to a directory _stroom-agent/lib_

Various scripts are used to run the agent.

## Running the Agent

_TODO_ fill this section in

## Configuration

The configuration for the agent is defined with a spring configuration file (examples are provided).

The main aspects to the configuration are:

* Define where you want the agent to send data to:

_TODO_ check stroom url for below

``` xml
<bean class="stroom.agent.dispatch.SimpleDispatcher">
    <property name="forwardUrl"
        value="http://stroom.someDomain/........" />
</bean>
```

* Where you want to buffer files once they have been collected before they are sent

``` xml
<bean class="stroom.util.zip.stroomZipRepository">
    <constructor-arg type="java.lang.String" value="${stroomAgentDir}/agent-repo-10min" />
</bean>
```

* One or more collectors (e.g)

``` xml
<bean class="stroom.agent.collect.PullSFTPCollector">
    <property name="feed" value="EXAMPLE_SYSTEM" />
    <property name="remoteDir" value="/source/data/path" />
    <property name="remoteFile" value=".*\.csv" />
    <property name="maxExpectedMatches" value="" />
    <property name="sftpTransfer" ref="singsing" />
    <!-- Set to true on live -->
    <property name="delete" value="false" />
    <property name="headerMapPopulatorEntryList" ref="exampleSystemHeader" />
</bean>
```

There are currently 3 types of collector:

* SimpleDBCollector

* PullSFTPCollector

* DayPullSFTPCollector

## SimpleDBCollector

This collector queries the database with a result set of event data.

It is assumed that some kind of sequence (Number, String) is in the first column.

The sequence of the last row read is stored in the idPersistFile and is used on the subsequent query to move the result set on.

If no initial sequence is known the fromDateQuery is used to select the sequence from a given start date.

It has the following properties

* **query** - The query to form the result set.  The first column is assumed to be the sequence

* **fromDateQuery** - The query to used when no start sequence is known (you must provide fromDate to the stroom-agent at start up)

* **jdbcDriver** 

* **jdbcUrl**

* **jdbcPassword**

* **jdbcUser**

* **idPersistFile** - The file to persist the current sequence to

* **feed** 

* **header** (default true) - Output the SQL result set column headers as a CSV header 

* **checkSequence** (default true)

* **debug** (default false) - Just output the results to system out.  Will not change the persistFile i.e. you can run and run again

* **stroomDate** (default true) - Convert any dates or timestamps in the result set to Stroom date format

* **maxRows** - Max number of results to export (can be used when debugging)

``` xml
<bean class="stroom.agent.collect.SimpleDBCollector">
    <property name="idPersistFile"
        value="${stroomAgentDir}/pull-id-ID-FILE" />
    <property name="fromDateQuery"
        value="select max(id_col) from some_table where timestamp_col &lt; ?" />
    <property name="query"
        value="select * from some_table where id_col > ? order by id_col" />

    <property name="jdbcDriver" value="com.microsoft.sqlserver.jdbc.SQLServerDriver" />
    <property name="jdbcUrl"
        value="jdbc:sqlserver://1.1.1.1:1433;databaseName=ExampleDatabase" />
    <property name="jdbcUser" value="user"/>
    <property name="jdbcPassword" value="myPassword" />
    <property name="feed" value="EXAMPLE-FEED" />
</bean>
```

## PullSFTPCollector

This collector can pull logs from a directory structure and optionally delete them on completion.

It assumes a success if the expected number of files are matched.

At has the following properties:

* **remoteDir** - A path name string where each part of the part can be a regular expression.  So /home/user/*. would imply all directories in /home/user

* **remoteFile** - A regular expression to match on for the file to transfer.  So *.\.log would be any .log file

* **feed** 

* **sftpTransfer** - The bean used for the transfer (see below)

* **sftpGetHandler** - The bean used to transfer individual files (see below)

* **minExpectedMatches** (default 1) - The min expected files (can be null for no check)

* **maxExpectedMatches** (default 1) - The max expected files (can be null for no check)

* **delete** (default false) - Delete the remote file on completion

* **renameSuffix** (default null) - File name to rename to once processed (rather than delete)

## SFTPTransfer

Bean used for the remote SFTP details with the following properties:

* **user** - SSH user id

* **host** - 

* **knownHosts** - SSH details (e.g. ${HOME}/.ssh/known_hosts)

* **identity** - SSH details (e.g. ${HOME}/.ssh/id_dsa)

* **validFileMTime** (default none) - Consider files by modifiy time (e.g. "2m" would only extract the matching file if the modifiy time was > 2 min)

* **validFileATime** (default none) - Consider files by access time 

* **validDirMTime** (default none) - Consider dynmaic directories (where regular expression provided) by modifiy time 

* **validDirATime** (default none) - Consider dynmaic directories by access time

## SFTPGetHandler

Bean used to transfer each file and can be one of:

* **SFTPGetHandlerDefault** - The default which assumes files are uncompressed

* **SFTPGetHandlerGZIP** - Assumes the files have basic gzip (java gzip) compression

* **SFTPGetHandlerTAR** - Used if the file is an archive.  You can additionally provide a file regular expression to match on.

* **SFTPGetHandlerZIP** - Used if the file is an archive.  You can additionally provide a file regular expression to match on.

* **SFTPGetHandlerCommonsCompressGZIP** - In the case where compound gzip compression is used (concatinated gzip files)

Example:

``` xml
<bean id="exampleSystemHost" class="stroom.agent.collect.SFTPTransfer">
    <property name="host" value="exampleHost.sub.domain.uk" />
    <property name="user" value="exampleuser" />
    <property name="knownHosts" value="${HOME}/.ssh/known_hosts" />
    <property name="identity" value="${HOME}/.ssh/id_dsa" />
    <property name="validFileMTime" value="1h" />
    <property name="validDirMTime" value="1h" />
</bean>

<bean id="zipHandler" class="stroom.agent.collect.SFTPGetHandlerZIP">
    <property name="file" value=".*only.*"/>
</bean>

<bean class="stroom.agent.collect.DayPullSFTPCollector">
    <property name="feed" value="EXAMPLE_SYSTEM" />
    <property name="dateFile" value="${stroomAgentDir}/pull-date-EXAMPLE_SYSTEM" />
    <property name="remoteDir" value="'/source/data/path/'" />
    <property name="remoteFile" value="yyyy-MM-dd'\.zip'" />
    <property name="sftpTransfer" ref="exampleSystemHost" />
    <property name="sftpGetHandler" ref="zipHandler" />
</bean>
```

## DayPullSFTPCollector

Extention to the PullSFTPCollector that additionally tracks the concept of transfering files per day.

If the transfer is not done for a particalar day it will retry that day when it is next run.  Has the additional properties:

* **remoteDir** - A java simple date format evaluated to a regular expression path.  E.g. `/data/.*/'yyyy-MM-dd'` would evaluate to `/data/.*/2012-01-01` and thus pick up for example `/data/myserver/2012-01-01`

* **remoteFile** - A java simple date format evaluated to a regular expression file match.  E.g. `'yyyy-MM-dd'\.log` would match on file `2012-01-01.log`

* **dateFile** - The file to use to store all the dates to transfer for.  Once a day transfer is sucessfull it will be removed from this file.  During normal processing the file will contain a line for each day that failed (e.g. weekends) and 2 furture dates (today + 1, today +2).

* **minAge** (default 1) - only pick up files from the previous n days.  For example by default if run on 2012-01-03 the latest day it will process is 2012-01-02 (1 day old).

* **maxAge** (default 30) - if a date fails then give up once it is longer that 30 days old.

Example:

``` xml
<bean id="exampleSystemHost" class="stroom.agent.collect.SFTPTransfer">
    <property name="host" value="exampleHost.sub.domain.uk" />
    <property name="user" value="exampleuser" />
    <property name="knownHosts" value="${HOME}/.ssh/known_hosts" />
    <property name="identity" value="${HOME}/.ssh/id_dsa" />
    <property name="validFileMTime" value="1h" />
    <property name="validDirMTime" value="1h" />
</bean>

<bean id="zipHandler" class="stroom.agent.collect.SFTPGetHandlerZIP">
    <property name="file" value=".*only.*"/>
</bean>

<bean class="stroom.agent.collect.DayPullSFTPCollector">
    <property name="feed" value="ACL-TXT-EVENTS" />
    <property name="dateFile" value="${stroomAgentDir}/pull-date-EXAMPLE_SYSTEM" />
    <property name="remoteDir" value="'/source/data/path/'" />
    <property name="remoteFile" value="yyyy-MM-dd'\.zip'" />
    <property name="sftpTransfer" ref="exampleSystemHost" />
    <property name="sftpGetHandler" ref="zipHandler" />
</bean>
```

## Extra Meta Arguments

For all the collectors you can provide extra meta arguments for the Stroom transfer.

They can be simple constants or evaluated based on existing meta arguments (for example the server name is embedded in the file name).

You need to set the headerMapPopulatorEntryList collector argument as below.

In the below example the following meta arguments would exist "System:EXAMPLE_SYSTEM", "Environment:OPS", "RemoteFile:/a/c/b/server1/log.txt", "DeviceHost:server1"

``` xml
<util:list id="exampleSystemHeader">

    <bean class="stroom.agent.collect.HeaderMapPopulatorEntry">
        <constructor-arg type="java.lang.String" value="System" />
        <constructor-arg type="java.lang.String" value="EXAMPLE_SYSTEM" />
    </bean>

    <bean class="stroom.agent.collect.HeaderMapPopulatorEntry">
        <constructor-arg type="java.lang.String" value="Environment" />
        <constructor-arg type="java.lang.String" value="OPS" />
    </bean>

    <bean class="stroom.agent.collect.HeaderMapPopulatorEntry">
        <constructor-arg type="java.lang.String" value="DeviceHost" />
        <constructor-arg type="java.lang.String" value="${4}.sub.domain.uk" />
        <constructor-arg type="java.lang.String" value="RemoteFile" />
        <constructor-arg type="java.lang.String" value="/([^/]*)/([^/]*)/([^/]*)/([^/]*).*" />
    </bean>

</util:list>

<bean class="stroom.agent.collect.DayPullSFTPCollector">
...
    <property name="headerMapPopulatorEntryList" ref="exampleSystemHeader" />
</bean>
```

