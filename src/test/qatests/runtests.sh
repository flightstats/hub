#mocha $(find . -type f -name '*test.js') -R spec --timeout 45000 $@
mocha ./Tests -R spec --timeout 45000 $@