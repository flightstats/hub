import logging
import sys

from lumberjack.handlers import GzippedTimedRotatingFileHandler


def setup_logging(level=logging.INFO, filename='unnamed.log'):
    root_logger = logging.getLogger()
    root_logger.setLevel(level)

    log_format = '%(asctime)s [%(levelname)s] %(name)s : %(message)s'
    formatter = logging.Formatter(log_format, '%Y-%m-%d %H:%M:%S')

    root_logger.addHandler(create_file_handler(formatter, filename))
    root_logger.addHandler(create_stream_handler(formatter))

    # squelch some 3rd party logging
    urllib_logger = logging.getLogger('urllib3.connectionpool')
    urllib_logger.setLevel(logging.WARNING)


def create_file_handler(formatter, filename):
    handler = GzippedTimedRotatingFileHandler(filename, when='midnight', interval=1, backupCount=7, encoding='utf-8', utc=True)
    handler.setFormatter(formatter)
    return handler


def create_stream_handler(formatter):
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(formatter)
    handler.setLevel(logging.INFO)
    return handler
