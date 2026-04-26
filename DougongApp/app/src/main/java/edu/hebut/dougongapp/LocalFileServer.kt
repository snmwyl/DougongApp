package edu.hebut.dougongapp

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

class LocalFileServer(private val rootDir: String) : NanoHTTPD(8080) {
    override fun serve(session: IHTTPSession): Response {
        val file = File(rootDir, session.uri)
        if (file.exists() && !file.isDirectory) {
            val response = newChunkedResponse(Response.Status.OK, "model/gltf-binary", FileInputStream(file))
            response.addHeader("Access-Control-Allow-Origin", "*")
            return response
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
    }
}