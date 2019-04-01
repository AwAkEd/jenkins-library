/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019
 */

import java.util.logging.Logger
import org.junit.*
import org.zowe.jenkins_shared_library.integrationtest.*
import static groovy.test.GroovyAssert.*

/**
 * Test {@link org.zowe.jenkins_shared_library.scm.GitHub}
 *
 * The test case will create a test Jenkins job and attach the current library to it.
 *
 * Then will run several validations on the job:
 *
 * - start with parameter pointing to the library branch to test
 */
class GitHubTest extends IntegrationTest {
    @BeforeAll
    void initTestJob() {
        super.initTestJob()

        // init test job name
        _initTestJobName('github')

        // create test job
        def envVars = """GITHUB_USERNAME=${System.getProperty('github.username')}
GITHUB_EMAIL=${System.getProperty('github.email')}
GITHUB_CREDENTIAL=${System.getProperty('github.credential')}
"""
        def script = Utils.loadResource('/pipelines/githubTest.groovy')
        api.createJob(
            testJobName,
            'pipeline.xml',
            [Constants.INTEGRATION_TEST_JENKINS_FOLDER],
            [
                'fvt-env-vars'     : Utils.escapeXml(envVars),
                'fvt-script'       : Utils.escapeXml(script),
            ]
        )

        buildResult = api.startJobAndGetResult(fullTestJobName, [
            'LIBRARY_BRANCH': System.getProperty('library.branch')
        ])

        if (buildResult && buildResult['number']) {
            buildLog = getBuildLog(fullTestJobName, buildResult['number'])
        }
    }

    @AfterAll
    void deleteTestJob() {
        // delete the test job if exists
        if (api && testJobName) {
            // api.deleteJob(fullTestJobName)
        }
    }

    @Test
    void testBuildResult() {
        assertTrue('Build result doesn\'t have \'number\' defined', buildResult.containsKey('number'))
        assertTrue('Build result doesn\'t have \'result\' defined', buildResult.containsKey('result'))
        assertTrue("Build result should be SUCESS but got ${buildResult['result']}", buildResult['result'] == 'SUCCESS')
        assertTrue('Build log is empty', buildLog != '')
    }

    @Test
    void testInit() {
        assertTrue(buildLog.contains('[GITHUB_TEST] init successfully'))
    }

    @Test
    void testClone() {
        assertTrue(buildLog.contains('[GITHUB_TEST] clone successfully'))
    }

    @Test
    void testCommit() {
        assertTrue(buildLog.contains('[GITHUB_TEST] commit successfully'))
    }

    @Test
    void testPush() {
        assertTrue(buildLog.contains('[GITHUB_TEST] push successfully'))
    }

    @Test
    void testTag() {
        assertTrue(buildLog.contains('[GITHUB_TEST] tag successfully'))
    }
}
