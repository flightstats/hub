import logging
from logging.handlers import TimedRotatingFileHandler

# todo - Figure out how to override Locust's logging.basicConfig call so we can control the console output.


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
    # https://docs.python.org/2/library/logging.handlers.html#timedrotatingfilehandler
    handler = logging.handlers.TimedRotatingFileHandler(log_file, when='midnight', interval=1, backupCount=7, encoding='utf-8', utc=True)
    handler.setFormatter(formatter)
    return handler
