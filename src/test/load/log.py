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

    def __init__(self, filename, when='h', interval=1, backupCount=0, encoding=None, delay=False, utc=False, extension='.gz'):
        TimedRotatingFileHandler.__init__(self, filename, when, interval, backupCount, encoding, delay, utc)
        self.compressed_extension = extension
        self.recompile_extension_matcher()

    def recompile_extension_matcher(self):
        self.original_matcher = self.extMatch
        new_pattern = self.original_matcher.pattern[:-1] + '{}$'.format(self.compressed_extension)
        self.extMatch = re.compile(new_pattern)

    def doRollover(self):
        TimedRotatingFileHandler.doRollover(self)
        file_paths = self.get_uncompressed_logs()
        for file_path in file_paths:
            self.compress(file_path)
            os.remove(file_path)

    def get_uncompressed_logs(self):
        dir_name, base_name = os.path.split(self.baseFilename)
        all_files = os.listdir(dir_name)
        return [os.path.join(dir_name, file_name) for file_name in all_files if self.is_uncompressed_rotated_log(file_name, base_name)]

    def is_uncompressed_rotated_log(self, filename, prefix):
        return self.is_log(filename, prefix) and self.is_rotated(filename, prefix) and self.is_uncompressed(filename)

    @staticmethod
    def is_log(filename, prefix):
        return filename.startswith(prefix)

    def is_rotated(self, filename, prefix):
        suffix = filename[len(prefix) + 1:]
        return self.original_matcher.match(suffix)

    def is_uncompressed(self, filename):
        return not filename.endswith(self.compressed_extension)

    def compress(self, uncompressed_filename):
        compressed_filename = uncompressed_filename + self.compressed_extension
        with open(uncompressed_filename, 'rb') as f_in, gzip.open(compressed_filename, 'wb') as f_out:
            shutil.copyfileobj(f_in, f_out)
