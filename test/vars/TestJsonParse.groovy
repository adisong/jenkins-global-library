package vars


import helpers.PipelineSpockTestBase

/**
 * Test jsonParse global var
 */
class TestJsonParse extends PipelineSpockTestBase {

    def "default"() {

        given:
        def params = "{\"property\":\"value\", \"list\": [\"item1\",\"item2\"], \"object\":{\"nestedProperty\":\"value\"}}"
        def expected = [
                property: "value",
                list: ["item1","item2"],
                object: [
                    nestedProperty: "value"
                ]
        ]

        when:
        def script = loadScript('vars/jsonParse.groovy')
        def actual = script.call(params)

        then:
        assertJobStatusSuccess()
        actual == expected
    }
}