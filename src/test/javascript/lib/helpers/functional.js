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
module.exports.getProp = (...args) => {
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
module.exports.fromObjectPath = (...args) => {
    const [path, obj] = args;
    if ((args.length < 2)) return obj => getFromObjectPath(path, obj);
    if (!obj) {
        return null;
    }
    return getFromObjectPath(path, obj);
};
