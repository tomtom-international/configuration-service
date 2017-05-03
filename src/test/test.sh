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
        echo "ERROR: '$LINE' does not match: '$REGEX'"
        echo "Test case failed!"
        echo ""
        echo "Is the service running at $HOST.*?"
        exit 1
    fi
}

echo "Request version -- Should contain version"
export RESULT=$(curl -s -X GET http://$HOST/version)
checkString "$RESULT" version

echo "Request status -- Should be empty"
export RESULT=$(curl -s -X GET http://$HOST/status)

echo "Request help -- Should be html"
export RESULT=$(curl -s -X GET http://$HOST)
checkString "$RESULT" \<html\>

echo ""
echo "Request entire tree"
export RESULT=$(curl -s -X GET http://$HOST/tree)
checkString "$RESULT" traffic.*Device1.*settings

echo ""
echo "Search for XYZ -- Should produce ApiNotFoundException"
export RESULT=$(curl -s -X GET http://$HOST/tree?service=XYZ)
checkString "$RESULT" ApiNotFound

echo ""
echo "Search for XYZ -- Should produce ApiForbiddenException"
export RESULT=$(curl -s -X GET http://$HOST/tree/settings?service=XYZ)
checkString "$RESULT" ApiForbidden

echo ""
echo "Search for traffic -- Should produce radius_km:50"
export RESULT=$(curl -s -X GET http://$HOST/tree?service=traffic)
checkString "$RESULT" radius_km.*\"50

echo ""
echo "Search for traffic/luxuri -- Should produce radius_km:50"
export RESULT=$(curl -s -X GET http://$HOST/tree?service=traffic\&model=luxuri)
checkString "$RESULT" radius_km.*\"50

echo ""
echo "Search for traffic/luxuri/Device123 -- Should produce radius_km:100"
export RESULT=$(curl -s -X GET http://$HOST/tree?service=traffic\&model=luxuri\&device=device123)
checkString "$RESULT" radius_km.*\"100

echo ""
echo "Search for traffic/cheapo/Device456 with earlier If-Modified-Since -- Should produce radius_km:40"
export RESULT=$(curl -s -H "If-Modified-Since: Mon, 2 Jan 2000 19:43:31 GMT" -X GET http://$HOST/tree?service=traffic\&model=cheapo\&device=device456)
checkString "$RESULT" radius_km.*\"40

echo ""
echo "Search for traffic/cheapo/Device456 with non-matching If-None-match -- Should produce radius_km:40"
export RESULT=$(curl -s -H "If-None-Match: \"x\"" -X GET http://$HOST/tree?service=traffic\&model=cheapo\&device=device456)
checkString "$RESULT" radius_km.*\"40

echo ""
echo "Search for traffic/cheapo/Device456 with matching If-None-match -- Should produce radius_km:40"
export RESULT=$(curl -s -H "If-None-Match: \"8c3754f2\"" -X GET http://$HOST/tree?service=traffic\&model=cheapo\&device=device456)
checkString x"$RESULT" x

echo ""
echo "Get node traffic/cheapo/Device456 with newer If-Modified-Since -- Should produce nothing"
export RESULT=$(curl -s -H "If-Modified-Since: Mon, 2 Jan 2020 19:43:31 GMT" -X GET http://$HOST/tree?service=traffic\&model=cheapo\&device=device456)
checkString x"$RESULT" x

echo ""
echo "Get node traffic/cheapo/Device456 with newer If-Modified-Since and non-matching If-None-Match -- Should produce radius_km:40"
export RESULT=$(curl -s -H "If-None-Match: \"x\"" -H "If-Modified-Since: Mon, 2 Jan 2020 19:43:31 GMT" -X GET http://$HOST/tree?service=traffic\&model=cheapo\&device=device456)
checkString "$RESULT" radius_km.*\"40

echo ""
echo "Get node traffic/cheapo/Device456 with newer If-Modified-Since and matching If-None-Match -- Should produce radius_km:40"
export RESULT=$(curl -s -H "If-None-Match: \"8c3754f2\"" -H "If-Modified-Since: Mon, 2 Jan 2020 19:43:31 GMT" -X GET http://$HOST/tree?service=traffic\&model=cheapo\&device=device456)
checkString x"$RESULT" x

echo ""
echo "Search for traffic/luxuri with earlier If-Modified-Since-- Should produce luxuri"
export RESULT=$(curl -s -H "If-Modified-Since: Mon, 2 Jan 2000 19:43:31 GMT" -X GET http://$HOST/tree/traffic/luxuri)
checkString "$RESULT" luxuri

echo ""
echo "Search for traffic/luxuri with newer If-Modified-Since-- Should produce nothing"
export RESULT=$(curl -s -H "If-Modified-Since: Mon, 2 Jan 2020 19:43:31 GMT" -X GET http://$HOST/tree/traffic/luxuri)
checkString x"$RESULT" x

echo ""
echo "Search for traffic/luxuri with empty If-Modified-Since-- Should produce luxuri"
export RESULT=$(curl -s -H "If-Modified-Since:" -X GET http://$HOST/tree/traffic/luxuri)
checkString "$RESULT" luxuri

echo ""
echo "Search for traffic/luxuri with wrong If-Modified-Since-- Should produce luxuri"
export RESULT=$(curl -s -H "If-Modified-Since: 123456789012" -X GET http://$HOST/tree/traffic/luxuri)
checkString "$RESULT" luxuri

echo ""
echo "Search for traffic,settings -- Should produce multiple results"
export RESULT=$(curl -s -X GET http://$HOST/tree?service=traffic,settings)
checkString "$RESULT" .*radius_km.*matched.*sound.*matched

echo "Test was successful"
