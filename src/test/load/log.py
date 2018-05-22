import gzip
import logging
import os
import re
import shutil
from logging.handlers import TimedRotatingFileHandler


def setup_logging(level=logging.INFO, filename='unnamed.log'):
    root_logger = logging.getLogger()
    root_logger.setLevel(level)

    log_format = '%(asctime)s [%(levelname)s] %(name)s : %(message)s'
    formatter = logging.Formatter(log_format, '%Y-%m-%d %H:%M:%S')
    root_logger.addHandler(create_file_handler(formatter, filename))

    # squelch some 3rd party logging
    urllib_logger = logging.getLogger('urllib3.connectionpool')
    urllib_logger.setLevel(logging.WARNING)


def create_file_handler(formatter, filename):
    handler = CompressedTimedRotatingFileHandler(filename, when='midnight', interval=1, backupCount=7, encoding='utf-8', utc=True)
    handler.setFormatter(formatter)
    return handler


class CompressedTimedRotatingFileHandler(TimedRotatingFileHandler):

    def __init__(self, filename, when='h', interval=1, backupCount=0, encoding=None, delay=False, utc=False):
        TimedRotatingFileHandler.__init__(self, filename, when, interval, backupCount, encoding, delay, utc)
        self.recompile_extension_matcher()

    def recompile_extension_matcher(self):
        original_pattern = self.extMatch.pattern
        new_pattern = original_pattern[:-1] + '.gz$'
        self.extMatch = re.compile(new_pattern)

    def doRollover(self):
        TimedRotatingFileHandler.doRollover(self)
        filenames = self.get_uncompressed_log_filenames()
        for filename in filenames:
            self.compress(filename)
            os.remove(filename)

    def get_uncompressed_log_filenames(self):
        dir_name, base_name = os.path.split(self.baseFilename)
        return [os.path.join(dir_name, filename) for filename in os.listdir(dir_name) if not filename.endswith('.log') and not filename.endswith('.gz')]

    @staticmethod
    def compress(uncompressed_filename):
        compressed_filename = '{}.gz'.format(uncompressed_filename)
        with open(uncompressed_filename, 'rb') as f_in, gzip.open(compressed_filename, 'wb') as f_out:
            shutil.copyfileobj(f_in, f_out)
