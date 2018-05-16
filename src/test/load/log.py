import logging
from logging.handlers import TimedRotatingFileHandler

# todo - Figure out how to override Locust's logging.basicConfig call so we can control the console output.

LOG_FILENAME = '/mnt/log/debug.log'


def setup_logging():
    urllib_logger = logging.getLogger('urllib3.connectionpool')
    urllib_logger.setLevel(logging.WARNING)

    # root_logger = logging.getLogger()
    # root_logger.setLevel(logging.DEBUG)

    # log_format = '%(asctime)s [%(levelname)s] %(name)s : %(message)s'
    # formatter = logging.Formatter(log_format, '%Y-%m-%d %H:%M:%S')
    # root_logger.addHandler(create_file_handler(formatter))


def create_file_handler(formatter):
    # https://docs.python.org/2/library/logging.handlers.html#timedrotatingfilehandler
    handler = logging.handlers.TimedRotatingFileHandler(LOG_FILENAME, when='midnight', interval=1, backupCount=7, encoding='utf-8', utc=True)
    handler.setFormatter(formatter)
    return handler
