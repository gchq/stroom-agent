# STROOM AGENT

Run example program as follows.

This example will pull the sample.txt file over sftp add it to the local zip repository.  It will then push the contents of the zip repository over SFP to the _out_ directory.


``` bash
./stroom-agent/bin/run.sh --classpath=./stroom-agent-example/conf --configfile=./stroom-agent-example/examples/ExampleConfig.xml
```

NB - This example requires ssh keys setup ()

``` bash
cd ~/.ssh
ssh-keygen -t dsa
cat id_dsa.pub >> authorized_keys
```

Verify ssh keys are setup correctly if the following logs in without prompting for a password

``` bash
ssh -i ~/.ssh/id_dsa localhost
```
