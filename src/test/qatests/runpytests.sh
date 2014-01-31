# Should be run in the folder above /Tests
curl http://artifactory.office/artifactory/qa/python_lib/FStest_helpers.py -o "./Tests/FStest_helpers.py"
curl http://artifactory.office/artifactory/qa/python_lib/unittest_helpers.py -o "./Tests/unittest_helpers.py"
nosetests -v ./Tests
