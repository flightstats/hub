// generic functional helpers

const getValueFromObjectByKey = (key, obj) =>
    (Object.prototype.hasOwnProperty.call(obj || {}, key) ? obj[key] : null);

/*
  (curried) takes a key: string and obj: { [string]: any } set of args
  returns the value of the key in obj or null
  ex's:
    getProp('foo', { foo: 'bar' }) // => bar
    getProp('foo', { baz: 'bar' }) // => null
    getProp('foo') // => function(obj) => return obj.foo || null
    getProp('foo', null) // => null
*/
const getProp = (...args) => {
    const [key, obj] = args;
    if (args.length < 2) return obj => getValueFromObjectByKey(key, obj);
    if (!obj) return null;
    return getValueFromObjectByKey(key, obj);
};

const getFromObjectPath = (path, obj) => path
    .reduce((accum, key) =>
        (accum ? accum[key] : accum), obj);
/*
  just like getProp above but takes an array representing a path to a nested object value
  ex: fromObjectPath(['foo', 'bar'], { foo: { bar: 'baz' } }); => 'baz'
*/
const fromObjectPath = (...args) => {
    const [path, obj] = args;
    if ((args.length < 2)) return obj => getFromObjectPath(path, obj);
    if (!obj) {
        return null;
    }
    return getFromObjectPath(path, obj);
};

// usage: async () => await itSleeps(500);
const itSleeps = (millis, message) => {
    process.stdout.write(`- -- waiting for --- ${millis / 1000} seconds ----`);
    const log = setInterval(() => process.stdout.write('-----'), (millis / 10));
    return new Promise((resolve) => {
        const timer = setTimeout(() => {
            clearInterval(log);
            clearTimeout(timer);
            if (message) console.log(message);
            return resolve(true);
        }, millis);
    });
};

// the drubbings will continue until the condition are belong to us...
const waitForCondition = async (conditionalFunc, timeout = 30000) => {
    let time = 0;
    if (typeof conditionalFunc !== 'function') return false;
    if (conditionalFunc()) return true;
    const resolver = () => new Promise((resolve) => {
        const wait = setInterval(() => {
            time += 500;
            const timedOut = time >= timeout;
            process.stdout.write('… … … … …');
            if (conditionalFunc() || timedOut) {
                if (timedOut) console.log('<<<< ERROR IN waitForCondition TIMED OUT >>>>');
                clearInterval(wait);
                return resolve(true);
            }
        }, 500);
    });
    await resolver();
};

const parseJson = (response, description) => {
    try {
        return JSON.parse(getProp('body', response)) || {};
    } catch (e) {
        const statusCode = getProp('body', response);
        const req = getProp('req', response) || {};
        console.log(
            'unable to parse json',
            statusCode,
            req.path,
            req.method,
            description,
            e && e.message
        );
        return response || {};
    }
};

module.exports = {
    fromObjectPath,
    getProp,
    itSleeps,
    parseJson,
    waitForCondition,
};
