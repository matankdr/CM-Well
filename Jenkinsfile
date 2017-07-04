pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        ansiColor(colorMapName: 'xterm') {
          dir(path: 'server') {
            sh '''rm -rf project/target
java -jar /opt/sbt/bin/sbt-launch.jar "reload plugins" update "reload return" clean reload compile'''
          }
          
        }
        
      }
    }
    stage('Test') {
      steps {
        ansiColor(colorMapName: 'xterm') {
          dir(path: 'server') {
            sh 'java -jar /opt/sbt/bin/sbt-launch.jar fullTest print-date'
            junit(testResults: '**/target/test-reports/*.xml', allowEmptyResults: true)
          }
          
        }
        
      }
    }
  }
}