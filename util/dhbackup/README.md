
# DataHub backup client

This client will back up a DataHub channel to a directory structure on disk.  Files are gzipped to save space.

## Installing

Run the setup.sh script.  This will do the following:

1. Create a virtualenv named `env`
2. Activate the virtualenv
3. Install the included dependencies in dhbackup.pybundle

## Running

```
$ source env/bin/activate
$ dhbackup.py -d /path/to/backup/root http://datahub.svc.xxx/channel/yourChannelName
```