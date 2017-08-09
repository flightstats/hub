# Javascript Tests

## Quickstart

    $ npm install
    ... dependencies installed ...    
    added 71 packages in 4.997s
    
    $ npm run
      Lifecycle scripts included in :
        test
          node jasmine
      
      available via `npm run-script`:
        integration-tests
          npm test -- integration/**/*_spec.js

## How to run a single test

    $ npm run test -- <spec> [optional parameters]

e.g.
    
    $ npm run test -- integration/channel_creation_basic_spec.js
