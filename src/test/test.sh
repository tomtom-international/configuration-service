#!/bin/sh
#
# Test script for additional test coverage.
#
# Run the server locally using:
# mvn clean jetty:run

HOST=localhost:8080

function checkString {
    LINE=$1
    REGEX=$2
    if [[ $LINE =~ $REGEX ]]
    then
        echo OK
    else
        echo ""
        echo "ERROR: '"$LINE"' does not match: '"$REGEX"'"
        echo "Test case failed!"
        echo ""
        echo "Is the service running at $HOST.*?"
        exit 1
    fi
}

echo "Request version -- Should contain version"
export RESULT=`curl -s -X GET http://$HOST/version`
checkString "$RESULT" version

echo "Request status -- Should be empty"
export RESULT=`curl -s -X GET http://$HOST/status`

echo "Request help -- Should be html"
export RESULT=`curl -s -X GET http://$HOST`
checkString "$RESULT" \<html\>

echo ""
echo "Request entire tree"
export RESULT=`curl -s -X GET http://$HOST/tree`
checkString "$RESULT" TPEG.*Device1.*SYS

echo ""
echo "Search for /XYZ -- Should produce ApiNotFoundException"
export RESULT=`curl -s -X GET http://$HOST/tree?levels=service\&search=XYZ`
checkString "$RESULT" ApiNotFound

echo ""
echo "Search for /XYZ -- Should produce ApiForbiddenException"
export RESULT=`curl -s -X GET http://$HOST/tree/SYS?levels=service\&search=XYZ`
checkString "$RESULT" ApiForbidden

echo ""
echo "Search for /TPEG -- Should produce radius:50"
export RESULT=`curl -s -X GET http://$HOST/tree?levels=service\&search=TPEG`
checkString "$RESULT" radius.*\"50

echo ""
echo "Search for /TPEG/P508 -- Should produce radius:50"
export RESULT=`curl -s -X GET http://$HOST/tree?levels=service/model\&search=TPEG/P508`
checkString "$RESULT" radius.*\"50

echo ""
echo "Search for /TPEG/P508/Device123 -- Should produce radius:100"
export RESULT=`curl -s -X GET http://$HOST/tree?levels=service/model/deviceID\&search=TPEG/P508/Device123`
checkString "$RESULT" radius.*\"100

echo ""
echo "Search for /TPEG/P107/Device456 with earlier If-Modified-Since -- Should produce radius:40"
export RESULT=`curl -s -H "If-Modified-Since: Mon, 2 Jan 2000 19:43:31 GMT" -X GET http://$HOST/tree?levels=service/model/deviceID\&search=TPEG/P107/Device456`
checkString "$RESULT" radius.*\"40

echo ""
echo "Search for /TPEG/P107/Device456 with non-matching If-None-match -- Should produce radius:40"
export RESULT=`curl -s -H "If-None-Match: \"x\"" -X GET http://$HOST/tree?levels=service/model/deviceID\&search=TPEG/P107/Device456`
checkString "$RESULT" radius.*\"40

echo ""
echo "Search for /TPEG/P107/Device456 with matching If-None-match -- Should produce radius:40"
export RESULT=`curl -s -H "If-None-Match: \"8c3754f2\"" -X GET http://$HOST/tree?levels=service/model/deviceID\&search=TPEG/P107/Device456`
checkString x"$RESULT" x

echo ""
echo "Get node /TPEG/P107/Device456 with newer If-Modified-Since -- Should produce nothing"
export RESULT=`curl -s -H "If-Modified-Since: Mon, 2 Jan 2020 19:43:31 GMT" -X GET http://$HOST/tree?levels=service/model/deviceID\&search=TPEG/P107/Device456`
checkString x"$RESULT" x

echo ""
echo "Get node /TPEG/P107/Device456 with newer If-Modified-Since and non-matching If-None-Match -- Should produce radius:40"
export RESULT=`curl -s -H "If-None-Match: \"x\"" -H "If-Modified-Since: Mon, 2 Jan 2020 19:43:31 GMT" -X GET http://$HOST/tree?levels=service/model/deviceID\&search=TPEG/P107/Device456`
checkString "$RESULT" radius.*\"40

echo ""
echo "Get node /TPEG/P107/Device456 with newer If-Modified-Since and matching If-None-Match -- Should produce radius:40"
export RESULT=`curl -s -H "If-None-Match: \"8c3754f2\"" -H "If-Modified-Since: Mon, 2 Jan 2020 19:43:31 GMT" -X GET http://$HOST/tree?levels=service/model/deviceID\&search=TPEG/P107/Device456`
checkString x"$RESULT" x

echo ""
echo "Search for /TPEG/P508 with earlier If-Modified-Since-- Should produce P508"
export RESULT=`curl -s -H "If-Modified-Since: Mon, 2 Jan 2000 19:43:31 GMT" -X GET http://$HOST/tree/TPEG/P508`
checkString "$RESULT" P508

echo ""
echo "Search for /TPEG/P508 with newer If-Modified-Since-- Should produce nothing"
export RESULT=`curl -s -H "If-Modified-Since: Mon, 2 Jan 2020 19:43:31 GMT" -X GET http://$HOST/tree/TPEG/P508`
checkString x"$RESULT" x

echo ""
echo "Search for /TPEG/P508 with empty If-Modified-Since-- Should produce P508"
export RESULT=`curl -s -H "If-Modified-Since:" -X GET http://$HOST/tree/TPEG/P508`
checkString "$RESULT" P508

echo ""
echo "Search for /TPEG/P508 with wrong If-Modified-Since-- Should produce P508"
export RESULT=`curl -s -H "If-Modified-Since: 123456789012" -X GET http://$HOST/tree/TPEG/P508`
checkString "$RESULT" P508

echo ""
echo "Search for TPEG,SYS -- Should produce multiple results"
export RESULT=`curl -s -X GET http://$HOST/tree?levels=service/model/deviceID\&search=TPEG\,SYS`
checkString "$RESULT" .*radius.*matched.*sound.*matched

echo "Test was successful"
