# DataHub backup client

This client will back up a DataHub channel to a directory structure on disk.  Files are gzipped to save space.  The backup client does not process existing channel content, only new content as it is being written.

## Installing

Extract the tgz archive and then run the setup.sh script.  This will do the following:

1. Create a virtualenv named `env`
2. Activate the virtualenv
3. Install the included dependencies in dhbackup.pybundle

## Running

```
$ source env/bin/activate
$ ./dhbackup.py \
    --dir /path/to/backup/root \
    --path "%Y/%m/%d/%H/%M/%S" \
    http://datahub.svc.xxx/channel/yourChannelName
```

If you need help:
```
$ ./dhbackup.py --help
usage: dhbackup.py [-h] -d DIR [-p PATH] channel-uri

positional arguments:
  channel-uri           The URI of the channel to back up

optional arguments:
  -h, --help            show this help message and exit
  -d DIR, --dir DIR     The directory to save data into
  -p PATH, --path PATH  The date format to use when creating subdirectories
                        (see python's strftime)
```
