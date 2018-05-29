import logging
import sys
from lumberjack.handlers import GzippedTimedRotatingFileHandler


def setup_logging(filename='unnamed.log'):
    log_format = '%(asctime)s [%(levelname)s] %(filename)s:%(lineno)d - %(message)s'
    formatter = logging.Formatter(log_format)

    # remove any root handlers setup by locust.io
    root_logger = logging.getLogger()
    root_logger.handlers = []

    # setup our own root handlers
    root_logger.setLevel(logging.NOTSET)
    root_logger.addHandler(create_stream_handler(formatter))
    root_logger.addHandler(create_file_handler(formatter, filename))

    # squelch some 3rd party logging
    urllib_logger = logging.getLogger('urllib3.connectionpool')
    urllib_logger.setLevel(logging.WARNING)


def create_file_handler(formatter, filename):
    handler = GzippedTimedRotatingFileHandler(filename, when='midnight', interval=1, backupCount=7, encoding='utf-8', utc=True)
    handler.setFormatter(formatter)
    handler.setLevel(logging.DEBUG)
    return handler


def create_stream_handler(formatter):
    handler = logging.StreamHandler(sys.__stdout__)
    handler.setFormatter(formatter)
    handler.setLevel(logging.INFO)
    return handler
