import gzip
import logging
import os
import shutil
from logging.handlers import TimedRotatingFileHandler


def setup_logging(level=logging.INFO, log_file='/mnt/locust.log'):
    root_logger = logging.getLogger()
    root_logger.setLevel(level)

    log_format = '%(asctime)s [%(levelname)s] %(name)s : %(message)s'
    formatter = logging.Formatter(log_format, '%Y-%m-%d %H:%M:%S')
    root_logger.addHandler(create_file_handler(formatter, log_file))

    # squelch some 3rd party logging
    urllib_logger = logging.getLogger('urllib3.connectionpool')
    urllib_logger.setLevel(logging.WARNING)


def create_file_handler(formatter, log_file):
    handler = CompressedTimedRotatingFileHandler(log_file, when='M', interval=1, backupCount=7, encoding='utf-8', utc=True)
    handler.setFormatter(formatter)
    return handler


class CompressedTimedRotatingFileHandler(TimedRotatingFileHandler):

    # adapted from: https://stackoverflow.com/a/43140586/157212

    def doRollover(self):
        TimedRotatingFileHandler.doRollover(self)
        dfn_uncompressed = self.find_most_recent_uncompressed_log()
        dfn_compressed = '{}.gz'.format(dfn_uncompressed)
        if os.path.exists(dfn_compressed):
            os.remove(dfn_compressed)
        with open(dfn_uncompressed, 'rb') as f_in, gzip.open(dfn_compressed, 'wb') as f_out:
            shutil.copyfileobj(f_in, f_out)
        os.remove(dfn_uncompressed)

    def find_most_recent_uncompressed_log(self):
        dir_name, base_name = os.path.split(self.baseFilename)
        uncompressed_files = [filename for filename in os.listdir(dir_name) if not filename.endswith('.log') and not filename.endswith('.gz')]
        uncompressed_files.sort()
        uncompressed_files.reverse()
        return uncompressed_files[0]
