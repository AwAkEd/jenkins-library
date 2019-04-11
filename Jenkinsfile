#!groovy

/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2019
 */

def isPullRequest = env.BRANCH_NAME.startsWith('PR-')

// only when building on these branches, docs will be updated
def DOCS_UPDATE_BRANCHES = ['master', 'classes']

// constants will be used for testing
def JENKINS_CREDENTIAL     = 'jenkins-credential'
def GITHUB_USERNAME        = 'Zowe Robot'
def GITHUB_EMAIL           = 'zowe.robot@gmail.com'
def GITHUB_CREDENTIAL      = 'zowe-robot-github'
def NPM_USERNAME           = 'giza-jenkins'
def NPM_EMAIL              = 'giza-jenkins@gmail.com'
def NPM_CREDENTIAL         = 'giza-jenkins-basicAuth'
def ARTIFACTORY_URL        = 'https://gizaartifactory.jfrog.io/gizaartifactory'
def ARTIFACTORY_CREDENTIAL = 'GizaArtifactory'
def PAX_SERVER_HOST        = 'river.zowe.org'
def PAX_SERVER_PORT        = 2022
def PAX_SERVER_CREDENTIAL  = 'ssh-zdt-test-image-guest'

def opts = []
// keep last 20 builds for regular branches, no keep for pull requests
opts.push(buildDiscarder(logRotator(numToKeepStr: (isPullRequest ? '' : '20'))))
// disable concurrent build
opts.push(disableConcurrentBuilds())

// define custom build parameters
def customParameters = []
customParameters.push(choice(
    name: 'TEST_LOG_LEVEL',
    choices: ['', 'SEVERE', 'WARNING', 'INFO', 'CONFIG', 'FINE', 'FINER', 'FINEST'],
    description: 'Log level for running gradle test. Default is INFO if leave it empty.'
))
opts.push(parameters(customParameters))

// set build properties
properties(opts)

node ('ibm-jenkins-slave-nvm-jnlp') {
    stage('checkout') {
        // checkout source code
        checkout scm

        // check if it's pull request
        echo "Current branch is ${env.BRANCH_NAME}"
        if (isPullRequest) {
          echo "This is a pull request"
        }

        // show env
        sh 'env'
    }

    stage('test') {
        try {
            withCredentials([usernamePassword(
                credentialsId: JENKINS_CREDENTIAL,
                passwordVariable: 'PASSWORD',
                usernameVariable: 'USERNAME'
            )]) {
                sh """
./gradlew test \
  -PlogLevel=${params.TEST_LOG_LEVEL} \
  -Pjenkins.baseuri='${env.JENKINS_URL}' \
  -Pjenkins.user='${USERNAME}' \
  -Pjenkins.password='${PASSWORD}' \
  -Plibrary.branch='${env.BRANCH_NAME}' \
  -Pgithub.username='${GITHUB_USERNAME}' \
  -Pgithub.email='${GITHUB_EMAIL}' \
  -Pgithub.credential='${GITHUB_CREDENTIAL}' \
  -Pnpm.username='${NPM_USERNAME}' \
  -Pnpm.email='${NPM_EMAIL}' \
  -Pnpm.credential='${NPM_CREDENTIAL}' \
  -Partifactory.url='${ARTIFACTORY_URL}' \
  -Partifactory.credential='${ARTIFACTORY_CREDENTIAL}' \
  -Ppax.server.host='${PAX_SERVER_HOST}' \
  -Ppax.server.port='${PAX_SERVER_PORT}' \
  -Ppax.server.crdential='${PAX_SERVER_CREDENTIAL}'
"""
            }
        } catch (e) {
            throw e
        } finally {
            // publish any test results found
            junit allowEmptyResults: true, testResults: '**/test-results/**/*.xml'

            // publish test html report
            publishHTML(target: [
                allowMissing         : false,
                alwaysLinkToLastBuild: false,
                keepAll              : true,
                reportDir            : 'build/reports/tests/test',
                reportFiles          : 'index.html',
                reportName           : "Unit Test Results"
            ])
        }
    }

    stage('render-doc') {
        if (!DOCS_UPDATE_BRANCHES.contains(env.BRANCH_NAME)) {
            echo "Skip building documentation for branch ${env.BRANCH_NAME}"
            return
        }

        // generate doc
        sh './gradlew groovydoc'

        // env.BRANCH_NAME
        dir('build/docs/groovydoc') {
            // init git folder
            sh """
git init
git remote add origin https://github.com/zowe/jenkins-library
git fetch origin
git reset origin/gh-pages
"""
            // check if there are changes on docs
            def docsUpdated = sh(script: "git status --porcelain", returnStdout: true).trim()
            if (docsUpdated) {
                echo "These documentation are changed:\n${docsUpdated}"
                // commit changes
                sh """git config user.email "${GITHUB_EMAIL}"
git config user.name "${GITHUB_USERNAME}"
git add .
git commit -m \"Updating docs from ${env.JOB_NAME}#${env.BUILD_NUMBER}\"
"""
                // push changes
                withCredentials([
                    usernamePassword(
                        credentialsId    : GITHUB_CREDENTIAL,
                        passwordVariable : 'PASSWORD',
                        usernameVariable : 'USERNAME'
                    )
                ]) {
                    sh "git push https://${USERNAME}:${PASSWORD}@github.com/zowe/jenkins-library HEAD:gh-pages"
                    echo "Documentation updated."
                }
            }
        }
    }
}
