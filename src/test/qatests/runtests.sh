curl http://artifactory.office/artifactory/qa/js_utils/randomUtils.js -o "randomUtils.js"
curl http://artifactory.office/artifactory/qa/js_utils/arrayHelpers.js -o "arrayHelpers.js"
curl http://artifactory.office/artifactory/qa/js_utils/contentTypes.js -o "contentTypes.js"
curl http://artifactory.office/artifactory/qa/js_utils/genericUtils.js -o "genericUtils.js"
#mocha $(find . -type f -name '*test.js') -R spec --timeout 45000 $@
mocha ./Tests -R spec --timeout 45000 $@
