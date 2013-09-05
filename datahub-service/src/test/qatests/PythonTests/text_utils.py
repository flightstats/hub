__author__ = 'gnewcomb'

import string
import random

def all_text_chars():
    ascii = range(48,58)
    ascii.extend(range(65,91))
    ascii.extend(range(97,123))
    ascii.extend(range(192,383))

    chars = ''
    for num in ascii:
        chars += unichr(num)
    return chars

def text_generator(size=0, chars=string.ascii_letters + string.digits +' ', min=1, max=50, includeSpace=True, doTrim=True):
    if (0 == size):
        if (min < 1):
            min = 1
        if (max < min):
            max = min
        size = random.randint(min, max)

    if (not includeSpace):
        chars = chars.replace(' ','')

    myStr = ''.join(random.choice(chars) for x in range(size))

    if (doTrim):
        myStr = myStr.strip()

        while (len(myStr) < min):
            myStr += random.choice(chars).strip()

    return myStr

