#mocha $(find . -type f -name '*test.js') -R spec --timeout 45000 $@
mocha --recursive ./ -R spec --timeout 45000 $@