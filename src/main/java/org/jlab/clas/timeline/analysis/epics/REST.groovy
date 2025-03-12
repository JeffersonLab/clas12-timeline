package org.jlab.clas.timeline.analysis

import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLContext
import javax.net.ssl.HttpsURLConnection
import groovy.json.JsonSlurper
import groovy.time.TimeCategory 
import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.H1F
import org.jlab.groot.data.GraphErrors

class REST {
  def sc = SSLContext.getInstance("SSL")
  def trustAll = [getAcceptedIssuers: {}, checkClientTrusted: { a, b -> }, checkServerTrusted: { a, b -> }]

  REST() {
    sc.init(null, [trustAll as X509TrustManager] as TrustManager[], null)
    HttpsURLConnection.defaultSSLSocketFactory = sc.socketFactory
  }


  static def get(String urlname) {
    def connection = new URL(urlname).openConnection() as HttpURLConnection
    connection.setRequestProperty( 'User-Agent', 'groovy-2' )
    connection.setRequestProperty( 'Accept', 'application/json' )

    if ( connection.responseCode == 200 ) {
      // get the JSON response
      def json = connection.inputStream.withCloseable { inStream ->
          new JsonSlurper().parse( inStream as InputStream )
      }
      return json
    }

    return null
  }
}

