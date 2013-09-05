__author__ = 'gnewcomb'

from functools import wraps
from multiprocessing import Process

class TimeoutError(Exception):
    pass

def timeout(seconds=5, error_message="Timeout"):
    def decorator(func):
        def wrapper(*args, **kwargs):
            process = Process(None, func, None, args, kwargs)
            process.start()
            process.join(seconds)
            if process.is_alive():
                process.terminate()
                raise TimeoutError(error_message)

        return wraps(func)(wrapper)
    return decorator